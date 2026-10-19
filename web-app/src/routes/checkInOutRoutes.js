const express = require('express');
const router = express.Router();
const { query, getClient } = require('../db');
const { authenticate } = require('../middleware/auth');
const { calculateBill } = require('../services/billingService');

// GET /api/checkin/queue - Reserved bookings ready for check-in
router.get('/queue', authenticate, async (req, res, next) => {
  try {
    const result = await query(`
      SELECT b.booking_id, b.customer_id, c.full_name AS customer_name, c.phone AS customer_phone,
             b.room_id, r.room_number, r.room_type, r.price_per_night,
             b.check_in_date, b.expected_check_out_date, b.number_of_guests, b.special_request,
             COALESCE(SUM(p.amount), 0)::numeric AS total_paid
      FROM bookings b
      JOIN customers c ON b.customer_id = c.customer_id
      JOIN rooms r ON b.room_id = r.room_id
      LEFT JOIN payments p ON b.booking_id = p.booking_id AND p.payment_status = 'PAID'
      WHERE b.booking_status = 'RESERVED'
      GROUP BY b.booking_id, c.full_name, c.phone, r.room_number, r.room_type, r.price_per_night
      ORDER BY b.check_in_date ASC, b.booking_id ASC
    `);

    res.json({
      success: true,
      queue: result.rows,
    });
  } catch (err) {
    next(err);
  }
});

// POST /api/checkin/:id - Check in guest
router.post('/confirm/:id', authenticate, async (req, res, next) => {
  const client = await getClient();
  try {
    const bookingId = req.params.id;

    const bookingRes = await client.query('SELECT * FROM bookings WHERE booking_id = $1', [bookingId]);
    if (bookingRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Booking not found.' });
    }
    const booking = bookingRes.rows[0];

    if (booking.booking_status !== 'RESERVED') {
      return res.status(400).json({
        success: false,
        message: `Only RESERVED bookings can be checked in. Current status: ${booking.booking_status}.`,
      });
    }

    await client.query('BEGIN');

    // Update booking status to CHECKED_IN
    await client.query(`
      UPDATE bookings 
      SET booking_status = 'CHECKED_IN' 
      WHERE booking_id = $1
    `, [bookingId]);

    // Update room status to OCCUPIED
    await client.query(`
      UPDATE rooms 
      SET status = 'OCCUPIED' 
      WHERE room_id = $1
    `, [booking.room_id]);

    await client.query('COMMIT');

    res.json({
      success: true,
      message: `Guest successfully checked in. Room marked as OCCUPIED.`,
    });
  } catch (err) {
    await client.query('ROLLBACK');
    next(err);
  } finally {
    client.release();
  }
});

// GET /api/checkout/active - Stays currently in-house (CHECKED_IN)
router.get('/active', authenticate, async (req, res, next) => {
  try {
    const result = await query(`
      SELECT b.booking_id, b.customer_id, c.full_name AS customer_name, c.phone AS customer_phone,
             b.room_id, r.room_number, r.room_type, r.price_per_night,
             b.check_in_date, b.expected_check_out_date, b.number_of_guests,
             COALESCE(SUM(p.amount), 0)::numeric AS total_paid
      FROM bookings b
      JOIN customers c ON b.customer_id = c.customer_id
      JOIN rooms r ON b.room_id = r.room_id
      LEFT JOIN payments p ON b.booking_id = p.booking_id AND p.payment_status = 'PAID'
      WHERE b.booking_status = 'CHECKED_IN'
      GROUP BY b.booking_id, c.full_name, c.phone, r.room_number, r.room_type, r.price_per_night
      ORDER BY b.check_in_date ASC
    `);

    res.json({
      success: true,
      activeStays: result.rows,
    });
  } catch (err) {
    next(err);
  }
});

// POST /api/checkout/calculate/:id - Compute itemized invoice
router.post('/calculate/:id', authenticate, async (req, res, next) => {
  try {
    const bookingId = req.params.id;
    const { additionalCharges = 0, taxRate = 0.12 } = req.body;

    const bRes = await query(`
      SELECT b.booking_id, b.customer_id, c.full_name AS customer_name, c.phone AS customer_phone,
             b.room_id, r.room_number, r.room_type, r.price_per_night,
             b.check_in_date, b.expected_check_out_date, b.booking_status
      FROM bookings b
      JOIN customers c ON b.customer_id = c.customer_id
      JOIN rooms r ON b.room_id = r.room_id
      WHERE b.booking_id = $1
    `, [bookingId]);

    if (bRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Booking not found.' });
    }
    const b = bRes.rows[0];

    const pRes = await query(`
      SELECT COALESCE(SUM(amount), 0)::numeric AS paid 
      FROM payments 
      WHERE booking_id = $1 AND payment_status = 'PAID'
    `, [bookingId]);
    const alreadyPaid = Number(pRes.rows[0]?.paid || 0);

    const bill = calculateBill({
      checkInDate: b.check_in_date,
      checkOutDate: new Date(),
      pricePerNight: b.price_per_night,
      additionalCharges,
      taxRate,
      alreadyPaid,
    });

    res.json({
      success: true,
      booking: b,
      bill,
    });
  } catch (err) {
    next(err);
  }
});

// POST /api/checkout/confirm/:id - Execute atomic checkout and release room
router.post('/confirm/:id', authenticate, async (req, res, next) => {
  const client = await getClient();
  try {
    const bookingId = req.params.id;
    const { additionalCharges = 0, taxRate = 0.12, paymentAmount = 0, paymentMethod = 'CASH', transactionReference } = req.body;

    const bRes = await client.query(`
      SELECT b.booking_id, b.customer_id, b.room_id, b.check_in_date, b.booking_status,
             r.room_number, r.price_per_night
      FROM bookings b
      JOIN rooms r ON b.room_id = r.room_id
      WHERE b.booking_id = $1
    `, [bookingId]);

    if (bRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Booking not found.' });
    }
    const b = bRes.rows[0];

    if (b.booking_status !== 'CHECKED_IN') {
      return res.status(400).json({
        success: false,
        message: `Only CHECKED_IN bookings can be checked out. Current status: ${b.booking_status}.`,
      });
    }

    const pRes = await client.query(`
      SELECT COALESCE(SUM(amount), 0)::numeric AS paid 
      FROM payments 
      WHERE booking_id = $1 AND payment_status = 'PAID'
    `, [bookingId]);
    const alreadyPaid = Number(pRes.rows[0]?.paid || 0);

    const bill = calculateBill({
      checkInDate: b.check_in_date,
      checkOutDate: new Date(),
      pricePerNight: b.price_per_night,
      additionalCharges,
      taxRate,
      alreadyPaid,
    });

    const payAmt = parseFloat(paymentAmount) || 0;
    const remainingAfterPay = bill.balanceDue - payAmt;

    if (remainingAfterPay > 0.05) {
      return res.status(400).json({
        success: false,
        message: `Outstanding balance of ₹${remainingAfterPay.toFixed(2)} must be fully settled before check-out.`,
        bill,
      });
    }

    await client.query('BEGIN');

    // 1. Record settlement payment if amount provided
    if (payAmt > 0) {
      await client.query(`
        INSERT INTO payments (booking_id, customer_id, amount, payment_date, payment_method, payment_status, transaction_reference)
        VALUES ($1, $2, $3, CURRENT_TIMESTAMP, $4, 'PAID', $5)
      `, [
        bookingId,
        b.customer_id,
        payAmt,
        paymentMethod,
        transactionReference || `CHECKOUT-SETTLE-${Date.now()}`
      ]);
    }

    // 2. Mark booking as CHECKED_OUT with actual date
    await client.query(`
      UPDATE bookings 
      SET booking_status = 'CHECKED_OUT', actual_check_out_date = CURRENT_DATE 
      WHERE booking_id = $1
    `, [bookingId]);

    // 3. Release room to AVAILABLE
    await client.query(`
      UPDATE rooms 
      SET status = 'AVAILABLE' 
      WHERE room_id = $1
    `, [b.room_id]);

    await client.query('COMMIT');

    res.json({
      success: true,
      message: `Check-out completed successfully for Room ${b.room_number}. Room released to AVAILABLE.`,
      finalBill: {
        ...bill,
        settlementPayment: payAmt,
        paymentMethod,
        actualCheckOutDate: new Date().toISOString().split('T')[0],
      },
    });
  } catch (err) {
    await client.query('ROLLBACK');
    next(err);
  } finally {
    client.release();
  }
});

module.exports = router;
