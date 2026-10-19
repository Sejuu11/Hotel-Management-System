const express = require('express');
const router = express.Router();
const { query } = require('../db');
const { authenticate } = require('../middleware/auth');

// 1. GET /api/reports/bookings - Reservations Ledger
router.get('/bookings', authenticate, async (req, res, next) => {
  try {
    const { fromDate, toDate, status } = req.query;
    let sql = `
      SELECT b.booking_id, c.full_name AS customer_name, c.phone AS customer_phone,
             r.room_number, r.room_type,
             b.check_in_date, b.expected_check_out_date, b.actual_check_out_date,
             b.booking_status,
             COALESCE(SUM(p.amount), 0)::numeric AS total_paid
      FROM bookings b
      JOIN customers c ON b.customer_id = c.customer_id
      JOIN rooms r ON b.room_id = r.room_id
      LEFT JOIN payments p ON b.booking_id = p.booking_id AND p.payment_status = 'PAID'
      WHERE 1=1
    `;
    const params = [];

    if (fromDate) {
      params.push(fromDate);
      sql += ` AND b.check_in_date >= $${params.length}`;
    }
    if (toDate) {
      params.push(toDate);
      sql += ` AND b.check_in_date <= $${params.length}`;
    }
    if (status && status !== 'ALL') {
      params.push(status);
      sql += ` AND b.booking_status = $${params.length}`;
    }

    sql += `
      GROUP BY b.booking_id, c.full_name, c.phone, r.room_number, r.room_type
      ORDER BY b.booking_id DESC
    `;

    const result = await query(sql, params);
    res.json({ success: true, report: result.rows });
  } catch (err) {
    next(err);
  }
});

// 2. GET /api/reports/revenue - Revenue Analytics with COUNT, SUM, AVG, MIN, MAX
router.get('/revenue', authenticate, async (req, res, next) => {
  try {
    const sql = `
      SELECT payment_date::date AS txn_date, payment_method,
             COUNT(*)::int AS payment_count,
             SUM(amount)::numeric AS total_revenue,
             ROUND(AVG(amount), 2)::numeric AS avg_payment,
             MIN(amount)::numeric AS min_payment,
             MAX(amount)::numeric AS max_payment
      FROM payments
      WHERE payment_status = 'PAID'
      GROUP BY payment_date::date, payment_method
      ORDER BY txn_date DESC, total_revenue DESC
    `;

    const result = await query(sql);
    res.json({ success: true, report: result.rows });
  } catch (err) {
    next(err);
  }
});

// 3. GET /api/reports/rooms - Room Utilization & Occupancy Report
router.get('/rooms', authenticate, async (req, res, next) => {
  try {
    const sql = `
      SELECT room_type,
             COUNT(*)::int AS total_rooms,
             SUM(CASE WHEN status = 'AVAILABLE' THEN 1 ELSE 0 END)::int AS available_count,
             SUM(CASE WHEN status = 'OCCUPIED' THEN 1 ELSE 0 END)::int AS occupied_count,
             SUM(CASE WHEN status = 'RESERVED' THEN 1 ELSE 0 END)::int AS reserved_count,
             SUM(CASE WHEN status = 'MAINTENANCE' THEN 1 ELSE 0 END)::int AS maintenance_count,
             ROUND(AVG(price_per_night), 2)::numeric AS avg_price,
             MIN(price_per_night)::numeric AS min_price,
             MAX(price_per_night)::numeric AS max_price
      FROM rooms
      GROUP BY room_type
      ORDER BY total_rooms DESC
    `;

    const result = await query(sql);
    res.json({ success: true, report: result.rows });
  } catch (err) {
    next(err);
  }
});

// 4. GET /api/reports/employees - Staff Payroll Breakdown by Designation
router.get('/employees', authenticate, async (req, res, next) => {
  try {
    const sql = `
      SELECT designation,
             COUNT(*)::int AS staff_count,
             SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END)::int AS active_count,
             SUM(salary)::numeric AS total_payroll,
             ROUND(AVG(salary), 2)::numeric AS avg_salary,
             MIN(salary)::numeric AS min_salary,
             MAX(salary)::numeric AS max_salary
      FROM employees
      GROUP BY designation
      ORDER BY total_payroll DESC
    `;

    const result = await query(sql);
    res.json({ success: true, report: result.rows });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
