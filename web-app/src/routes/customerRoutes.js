const express = require('express');
const router = express.Router();
const { query } = require('../db');
const { authenticate } = require('../middleware/auth');

// GET /api/customers - List customers with search
router.get('/', authenticate, async (req, res, next) => {
  try {
    const { search } = req.query;
    let sql = 'SELECT * FROM customers WHERE 1=1';
    const params = [];

    if (search && search.trim()) {
      params.push(`%${search.trim()}%`);
      sql += ` AND (full_name ILIKE $${params.length} OR phone ILIKE $${params.length} OR id_number ILIKE $${params.length})`;
    }

    sql += ' ORDER BY customer_id DESC';

    const result = await query(sql, params);
    res.json({
      success: true,
      customers: result.rows,
    });
  } catch (err) {
    next(err);
  }
});

// GET /api/customers/:id - Customer profile and booking history
router.get('/:id', authenticate, async (req, res, next) => {
  try {
    const customerId = req.params.id;
    const custRes = await query('SELECT * FROM customers WHERE customer_id = $1', [customerId]);
    if (custRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Customer not found.' });
    }

    // Fetch booking history
    const historyRes = await query(`
      SELECT b.booking_id, b.check_in_date, b.expected_check_out_date, b.actual_check_out_date,
             b.booking_status, r.room_number, r.room_type, r.price_per_night,
             COALESCE(SUM(p.amount), 0)::numeric AS total_paid
      FROM bookings b
      JOIN rooms r ON b.room_id = r.room_id
      LEFT JOIN payments p ON b.booking_id = p.booking_id AND p.payment_status = 'PAID'
      WHERE b.customer_id = $1
      GROUP BY b.booking_id, r.room_number, r.room_type, r.price_per_night
      ORDER BY b.booking_id DESC
    `, [customerId]);

    res.json({
      success: true,
      customer: custRes.rows[0],
      bookings: historyRes.rows,
    });
  } catch (err) {
    next(err);
  }
});

// POST /api/customers - Register new guest
router.post('/', authenticate, async (req, res, next) => {
  try {
    const { fullName, gender, dateOfBirth, phone, email, address, idType, idNumber } = req.body;

    if (!fullName || !gender || !phone || !idType || !idNumber) {
      return res.status(400).json({
        success: false,
        message: 'Full name, gender, phone, ID type, and ID number are required.',
      });
    }

    const insertRes = await query(`
      INSERT INTO customers (full_name, gender, date_of_birth, phone, email, address, id_type, id_number)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
      RETURNING *
    `, [
      fullName.trim(),
      gender,
      dateOfBirth || null,
      phone.trim(),
      email ? email.trim() : null,
      address ? address.trim() : null,
      idType,
      idNumber.trim(),
    ]);

    res.status(201).json({
      success: true,
      message: 'Customer registered successfully.',
      customer: insertRes.rows[0],
    });
  } catch (err) {
    next(err);
  }
});

// PUT /api/customers/:id - Update guest profile
router.put('/:id', authenticate, async (req, res, next) => {
  try {
    const customerId = req.params.id;
    const { fullName, gender, dateOfBirth, phone, email, address, idType, idNumber } = req.body;

    const updateRes = await query(`
      UPDATE customers
      SET full_name = COALESCE($1, full_name),
          gender = COALESCE($2, gender),
          date_of_birth = COALESCE($3, date_of_birth),
          phone = COALESCE($4, phone),
          email = COALESCE($5, email),
          address = COALESCE($6, address),
          id_type = COALESCE($7, id_type),
          id_number = COALESCE($8, id_number)
      WHERE customer_id = $9
      RETURNING *
    `, [
      fullName ? fullName.trim() : null,
      gender || null,
      dateOfBirth || null,
      phone ? phone.trim() : null,
      email !== undefined ? email.trim() : null,
      address !== undefined ? address.trim() : null,
      idType || null,
      idNumber ? idNumber.trim() : null,
      customerId,
    ]);

    if (updateRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Customer not found.' });
    }

    res.json({
      success: true,
      message: 'Customer updated successfully.',
      customer: updateRes.rows[0],
    });
  } catch (err) {
    next(err);
  }
});

// DELETE /api/customers/:id - Delete customer
router.delete('/:id', authenticate, async (req, res, next) => {
  try {
    const customerId = req.params.id;

    // Check active bookings
    const activeCheck = await query(`
      SELECT booking_id FROM bookings
      WHERE customer_id = $1 AND booking_status IN ('RESERVED', 'CHECKED_IN')
    `, [customerId]);

    if (activeCheck.rows.length > 0) {
      return res.status(400).json({
        success: false,
        message: 'Cannot delete customer with active reservations or checked-in stays.',
      });
    }

    const deleteRes = await query('DELETE FROM customers WHERE customer_id = $1 RETURNING full_name', [customerId]);
    if (deleteRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Customer not found.' });
    }

    res.json({
      success: true,
      message: `Customer ${deleteRes.rows[0].full_name} deleted successfully.`,
    });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
