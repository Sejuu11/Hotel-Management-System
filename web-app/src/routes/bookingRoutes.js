const express = require('express');
const router = express.Router();
const { query, getClient } = require('../db');
const { authenticate } = require('../middleware/auth');
const { validateBookingDates, isRoomBookedForDates } = require('../services/bookingValidator');

// GET /api/bookings - List all bookings with customer and room details
router.get('/', authenticate, async (req, res, next) => {
  try {
    const { status, search } = req.query;
    let sql = `
      SELECT b.booking_id, b.customer_id, c.full_name AS customer_name, c.phone AS customer_phone,
             b.room_id, r.room_number, r.room_type, r.price_per_night,
             b.booking_date, b.check_in_date, b.expected_check_out_date, b.actual_check_out_date,
             b.number_of_guests, b.booking_status, b.special_request,
             COALESCE(SUM(p.amount), 0)::numeric AS total_paid
      FROM bookings b
      JOIN customers c ON b.customer_id = c.customer_id
      JOIN rooms r ON b.room_id = r.room_id
      LEFT JOIN payments p ON b.booking_id = p.booking_id AND p.payment_status = 'PAID'
      WHERE 1=1
    `;
    const params = [];

    if (status && status !== 'ALL') {
      params.push(status);
      sql += ` AND b.booking_status = $${params.length}`;
    }

    if (search && search.trim()) {
      params.push(`%${search.trim()}%`);
      sql += ` AND (c.full_name ILIKE $${params.length} OR c.phone ILIKE $${params.length} OR r.room_number ILIKE $${params.length})`;
    }

    sql += `
      GROUP BY b.booking_id, c.full_name, c.phone, r.room_number, r.room_type, r.price_per_night
      ORDER BY b.booking_id DESC
    `;

    const result = await query(sql, params);
    res.json({
      success: true,
      bookings: result.rows,
    });
  } catch (err) {
    next(err);
  }
});

// GET /api/bookings/:id - Single booking details
router.get('/:id', authenticate, async (req, res, next) => {
  try {
    const bookingId = req.params.id;
    const result = await query(`
      SELECT b.*, c.full_name AS customer_name, c.phone AS customer_phone, c.email AS customer_email,
             r.room_number, r.room_type, r.price_per_night,
             COALESCE(SUM(p.amount), 0)::numeric AS total_paid
      FROM bookings b
      JOIN customers c ON b.customer_id = c.customer_id
      JOIN rooms r ON b.room_id = r.room_id
      LEFT JOIN payments p ON b.booking_id = p.booking_id AND p.payment_status = 'PAID'
      WHERE b.booking_id = $1
      GROUP BY b.booking_id, c.full_name, c.phone, c.email, r.room_number, r.room_type, r.price_per_night
    `, [bookingId]);

    if (result.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Booking not found.' });
    }

    res.json({ success: true, booking: result.rows[0] });
  } catch (err) {
    next(err);
  }
});

// POST /api/bookings - Create new room reservation
router.post('/', authenticate, async (req, res, next) => {
  const client = await getClient();
  try {
    const { customerId, roomId, checkInDate, expectedCheckOutDate, numberOfGuests = 1, specialRequest, advancePayment, paymentMethod } = req.body;

    if (!customerId || !roomId || !checkInDate || !expectedCheckOutDate) {
      return res.status(400).json({
        success: false,
        message: 'Customer, room, check-in date, and check-out date are required.',
      });
    }

    // 1. Date validation
    validateBookingDates(checkInDate, expectedCheckOutDate);

    // 2. Customer exists
    const custRes = await client.query('SELECT customer_id FROM customers WHERE customer_id = $1', [customerId]);
    if (custRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Selected customer does not exist.' });
    }

    // 3. Room exists and check current status
    const roomRes = await client.query('SELECT room_id, room_number, status, price_per_night FROM rooms WHERE room_id = $1', [roomId]);
    if (roomRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Selected room does not exist.' });
    }
    const room = roomRes.rows[0];

    // 4. Overlap collision check
    const isBooked = await isRoomBookedForDates(
      (sql, params) => client.query(sql, params),
      roomId,
      checkInDate,
      expectedCheckOutDate
    );
    if (isBooked) {
      return res.status(409).json({
        success: false,
        message: `Room ${room.room_number} is already booked or occupied for the selected dates.`,
      });
    }

    await client.query('BEGIN');

    // Insert booking
    const bookingRes = await client.query(`
      INSERT INTO bookings (customer_id, room_id, booking_date, check_in_date, expected_check_out_date, number_of_guests, booking_status, special_request, created_by)
      VALUES ($1, $2, CURRENT_DATE, $3, $4, $5, 'RESERVED', $6, $7)
      RETURNING *
    `, [
      customerId,
      roomId,
      checkInDate,
      expectedCheckOutDate,
      parseInt(numberOfGuests, 10) || 1,
      specialRequest || null,
      req.user.id || null,
    ]);
    const newBooking = bookingRes.rows[0];

    // Update room status to RESERVED if currently AVAILABLE
    await client.query(`UPDATE rooms SET status = 'RESERVED' WHERE room_id = $1 AND status = 'AVAILABLE'`, [roomId]);

    // Optional advance payment recording
    const advance = parseFloat(advancePayment);
    if (advance && advance > 0) {
      await client.query(`
        INSERT INTO payments (booking_id, customer_id, amount, payment_date, payment_method, payment_status, transaction_reference)
        VALUES ($1, $2, $3, CURRENT_TIMESTAMP, $4, 'PARTIAL', $5)
      `, [
        newBooking.booking_id,
        customerId,
        advance,
        paymentMethod || 'CASH',
        `ADV-PAY-${Date.now()}`
      ]);
    }

    await client.query('COMMIT');

    res.status(201).json({
      success: true,
      message: `Reservation confirmed for Room ${room.room_number}.`,
      booking: newBooking,
    });
  } catch (err) {
    await client.query('ROLLBACK');
    next(err);
  } finally {
    client.release();
  }
});

// PUT /api/bookings/:id/cancel - Cancel reservation and release room
router.put('/:id/cancel', authenticate, async (req, res, next) => {
  const client = await getClient();
  try {
    const bookingId = req.params.id;

    const bookingRes = await client.query('SELECT * FROM bookings WHERE booking_id = $1', [bookingId]);
    if (bookingRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Booking not found.' });
    }
    const booking = bookingRes.rows[0];

    if (booking.booking_status === 'CHECKED_OUT') {
      return res.status(400).json({ success: false, message: 'Cannot cancel a booking that has already completed checkout.' });
    }
    if (booking.booking_status === 'CANCELLED') {
      return res.status(400).json({ success: false, message: 'This reservation is already cancelled.' });
    }

    await client.query('BEGIN');

    // Update booking status
    await client.query(`
      UPDATE bookings 
      SET booking_status = 'CANCELLED', actual_check_out_date = CURRENT_DATE 
      WHERE booking_id = $1
    `, [bookingId]);

    // Release room back to AVAILABLE
    await client.query(`
      UPDATE rooms 
      SET status = 'AVAILABLE' 
      WHERE room_id = $1
    `, [booking.room_id]);

    await client.query('COMMIT');

    res.json({
      success: true,
      message: 'Reservation cancelled and room released to AVAILABLE.',
    });
  } catch (err) {
    await client.query('ROLLBACK');
    next(err);
  } finally {
    client.release();
  }
});

module.exports = router;
