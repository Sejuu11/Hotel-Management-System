const express = require('express');
const router = express.Router();
const { query } = require('../db');
const { authenticate, requireRole } = require('../middleware/auth');

// GET /api/rooms - List all rooms with optional filtering
router.get('/', authenticate, async (req, res, next) => {
  try {
    const { status, type, search } = req.query;
    let sql = 'SELECT * FROM rooms WHERE 1=1';
    const params = [];

    if (status && status !== 'ALL') {
      params.push(status);
      sql += ` AND status = $${params.length}`;
    }
    if (type && type !== 'ALL') {
      params.push(type);
      sql += ` AND room_type = $${params.length}`;
    }
    if (search) {
      params.push(`%${search.trim()}%`);
      sql += ` AND room_number ILIKE $${params.length}`;
    }

    sql += ' ORDER BY floor ASC, room_number ASC';

    const result = await query(sql, params);
    res.json({
      success: true,
      rooms: result.rows,
    });
  } catch (err) {
    next(err);
  }
});

// GET /api/rooms/:id - Get room by ID
router.get('/:id', authenticate, async (req, res, next) => {
  try {
    const result = await query('SELECT * FROM rooms WHERE room_id = $1', [req.params.id]);
    if (result.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Room not found.' });
    }
    res.json({ success: true, room: result.rows[0] });
  } catch (err) {
    next(err);
  }
});

// POST /api/rooms - Add a new room
router.post('/', authenticate, async (req, res, next) => {
  try {
    const { roomNumber, roomType, floor, pricePerNight, status = 'AVAILABLE', description } = req.body;

    if (!roomNumber || !roomType || floor === undefined || !pricePerNight) {
      return res.status(400).json({
        success: false,
        message: 'Room number, type, floor, and price per night are required.',
      });
    }

    const price = parseFloat(pricePerNight);
    if (isNaN(price) || price <= 0) {
      return res.status(400).json({
        success: false,
        message: 'Price per night must be a positive number.',
      });
    }

    // Check duplicate room number
    const duplicateCheck = await query('SELECT room_id FROM rooms WHERE room_number = $1', [roomNumber.trim()]);
    if (duplicateCheck.rows.length > 0) {
      return res.status(409).json({
        success: false,
        message: `Room number "${roomNumber.trim()}" already exists.`,
      });
    }

    const insertRes = await query(`
      INSERT INTO rooms (room_number, room_type, floor, price_per_night, status, description)
      VALUES ($1, $2, $3, $4, $5, $6)
      RETURNING *
    `, [roomNumber.trim(), roomType, parseInt(floor, 10), price, status, description || '']);

    res.status(201).json({
      success: true,
      message: 'Room added successfully.',
      room: insertRes.rows[0],
    });
  } catch (err) {
    next(err);
  }
});

// PUT /api/rooms/:id - Update an existing room
router.put('/:id', authenticate, async (req, res, next) => {
  try {
    const roomId = req.params.id;
    const { roomNumber, roomType, floor, pricePerNight, status, description } = req.body;

    // Check duplicate room number for other rooms
    if (roomNumber) {
      const duplicateCheck = await query(
        'SELECT room_id FROM rooms WHERE room_number = $1 AND room_id != $2',
        [roomNumber.trim(), roomId]
      );
      if (duplicateCheck.rows.length > 0) {
        return res.status(409).json({
          success: false,
          message: `Room number "${roomNumber.trim()}" is already assigned to another room.`,
        });
      }
    }

    const updateRes = await query(`
      UPDATE rooms
      SET room_number = COALESCE($1, room_number),
          room_type = COALESCE($2, room_type),
          floor = COALESCE($3, floor),
          price_per_night = COALESCE($4, price_per_night),
          status = COALESCE($5, status),
          description = COALESCE($6, description)
      WHERE room_id = $7
      RETURNING *
    `, [
      roomNumber ? roomNumber.trim() : null,
      roomType || null,
      floor !== undefined ? parseInt(floor, 10) : null,
      pricePerNight ? parseFloat(pricePerNight) : null,
      status || null,
      description !== undefined ? description : null,
      roomId,
    ]);

    if (updateRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Room not found.' });
    }

    res.json({
      success: true,
      message: 'Room updated successfully.',
      room: updateRes.rows[0],
    });
  } catch (err) {
    next(err);
  }
});

// DELETE /api/rooms/:id - Delete room if no active bookings exist
router.delete('/:id', authenticate, async (req, res, next) => {
  try {
    const roomId = req.params.id;

    // Verify room has no active reservations or active stays
    const activeBookingCheck = await query(`
      SELECT booking_id, booking_status 
      FROM bookings 
      WHERE room_id = $1 AND booking_status IN ('RESERVED', 'CHECKED_IN')
    `, [roomId]);

    if (activeBookingCheck.rows.length > 0) {
      return res.status(400).json({
        success: false,
        message: 'Cannot delete room with active reservations or occupied guests. Cancel or check out the booking first.',
      });
    }

    const deleteRes = await query('DELETE FROM rooms WHERE room_id = $1 RETURNING room_number', [roomId]);
    if (deleteRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Room not found.' });
    }

    res.json({
      success: true,
      message: `Room ${deleteRes.rows[0].room_number} deleted successfully.`,
    });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
