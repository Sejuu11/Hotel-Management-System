const express = require('express');
const router = express.Router();
const { query } = require('../db');
const { authenticate, requireRole } = require('../middleware/auth');

// All employee routes are restricted to ADMIN
router.use(authenticate, requireRole('ADMIN'));

// GET /api/employees - List all employees
router.get('/', async (req, res, next) => {
  try {
    const { designation, status, search } = req.query;
    let sql = 'SELECT * FROM employees WHERE 1=1';
    const params = [];

    if (designation && designation !== 'ALL') {
      params.push(designation);
      sql += ` AND designation = $${params.length}`;
    }
    if (status && status !== 'ALL') {
      params.push(status);
      sql += ` AND status = $${params.length}`;
    }
    if (search && search.trim()) {
      params.push(`%${search.trim()}%`);
      sql += ` AND (full_name ILIKE $${params.length} OR phone ILIKE $${params.length} OR email ILIKE $${params.length})`;
    }

    sql += ' ORDER BY employee_id ASC';

    const result = await query(sql, params);
    res.json({ success: true, employees: result.rows });
  } catch (err) {
    next(err);
  }
});

// GET /api/employees/:id - Single employee
router.get('/:id', async (req, res, next) => {
  try {
    const result = await query('SELECT * FROM employees WHERE employee_id = $1', [req.params.id]);
    if (result.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Employee not found.' });
    }
    res.json({ success: true, employee: result.rows[0] });
  } catch (err) {
    next(err);
  }
});

// POST /api/employees - Add new employee
router.post('/', async (req, res, next) => {
  try {
    const { fullName, gender, phone, email, address, designation, salary, joiningDate, status = 'ACTIVE' } = req.body;

    if (!fullName || !gender || !phone || !designation || !salary || !joiningDate) {
      return res.status(400).json({
        success: false,
        message: 'Full name, gender, phone, designation, salary, and joining date are required.',
      });
    }

    const sal = parseFloat(salary);
    if (isNaN(sal) || sal <= 0) {
      return res.status(400).json({ success: false, message: 'Salary must be a positive number.' });
    }

    const insertRes = await query(`
      INSERT INTO employees (full_name, gender, phone, email, address, designation, salary, joining_date, status)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
      RETURNING *
    `, [
      fullName.trim(),
      gender,
      phone.trim(),
      email ? email.trim() : null,
      address ? address.trim() : null,
      designation.trim(),
      sal,
      joiningDate,
      status,
    ]);

    res.status(201).json({
      success: true,
      message: 'Employee added successfully.',
      employee: insertRes.rows[0],
    });
  } catch (err) {
    next(err);
  }
});

// PUT /api/employees/:id - Update employee
router.put('/:id', async (req, res, next) => {
  try {
    const employeeId = req.params.id;
    const { fullName, gender, phone, email, address, designation, salary, joiningDate, status } = req.body;

    const updateRes = await query(`
      UPDATE employees
      SET full_name = COALESCE($1, full_name),
          gender = COALESCE($2, gender),
          phone = COALESCE($3, phone),
          email = COALESCE($4, email),
          address = COALESCE($5, address),
          designation = COALESCE($6, designation),
          salary = COALESCE($7, salary),
          joining_date = COALESCE($8, joining_date),
          status = COALESCE($9, status)
      WHERE employee_id = $10
      RETURNING *
    `, [
      fullName ? fullName.trim() : null,
      gender || null,
      phone ? phone.trim() : null,
      email !== undefined ? email.trim() : null,
      address !== undefined ? address.trim() : null,
      designation ? designation.trim() : null,
      salary ? parseFloat(salary) : null,
      joiningDate || null,
      status || null,
      employeeId,
    ]);

    if (updateRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Employee not found.' });
    }

    res.json({
      success: true,
      message: 'Employee updated successfully.',
      employee: updateRes.rows[0],
    });
  } catch (err) {
    next(err);
  }
});

// DELETE /api/employees/:id - Delete employee
router.delete('/:id', async (req, res, next) => {
  try {
    const deleteRes = await query('DELETE FROM employees WHERE employee_id = $1 RETURNING full_name', [req.params.id]);
    if (deleteRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Employee not found.' });
    }
    res.json({
      success: true,
      message: `Employee ${deleteRes.rows[0].full_name} removed successfully.`,
    });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
