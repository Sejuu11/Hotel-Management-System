const express = require('express');
const router = express.Router();
const { query } = require('../db');
const { authenticate } = require('../middleware/auth');

// GET /api/payments - List payments with filters
router.get('/', authenticate, async (req, res, next) => {
  try {
    const { method, search } = req.query;
    let sql = `
      SELECT p.*, c.full_name AS customer_name, c.phone AS customer_phone,
             r.room_number, b.booking_status
      FROM payments p
      JOIN customers c ON p.customer_id = c.customer_id
      JOIN bookings b ON p.booking_id = b.booking_id
      JOIN rooms r ON b.room_id = r.room_id
      WHERE 1=1
    `;
    const params = [];

    if (method && method !== 'ALL') {
      params.push(method);
      sql += ` AND p.payment_method = $${params.length}`;
    }

    if (search && search.trim()) {
      params.push(`%${search.trim()}%`);
      sql += ` AND (c.full_name ILIKE $${params.length} OR p.transaction_reference ILIKE $${params.length} OR r.room_number ILIKE $${params.length})`;
    }

    sql += ' ORDER BY p.payment_date DESC';

    const result = await query(sql, params);
    res.json({
      success: true,
      payments: result.rows,
    });
  } catch (err) {
    next(err);
  }
});

// POST /api/payments - Record a payment
router.post('/', authenticate, async (req, res, next) => {
  try {
    const { bookingId, customerId, amount, paymentMethod = 'CASH', paymentStatus = 'PAID', transactionReference } = req.body;

    if (!bookingId || !customerId || !amount) {
      return res.status(400).json({
        success: false,
        message: 'Booking ID, Customer ID, and Amount are required.',
      });
    }

    const payVal = parseFloat(amount);
    if (isNaN(payVal) || payVal <= 0) {
      return res.status(400).json({
        success: false,
        message: 'Payment amount must be greater than zero.',
      });
    }

    const insertRes = await query(`
      INSERT INTO payments (booking_id, customer_id, amount, payment_date, payment_method, payment_status, transaction_reference)
      VALUES ($1, $2, $3, CURRENT_TIMESTAMP, $4, $5, $6)
      RETURNING *
    `, [
      bookingId,
      customerId,
      payVal,
      paymentMethod,
      paymentStatus,
      transactionReference || `TXN-${Date.now()}`
    ]);

    res.status(201).json({
      success: true,
      message: 'Payment recorded successfully.',
      payment: insertRes.rows[0],
    });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
