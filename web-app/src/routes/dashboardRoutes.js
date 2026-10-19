const express = require('express');
const router = express.Router();
const { query } = require('../db');
const { authenticate } = require('../middleware/auth');

// GET /api/dashboard/stats
router.get('/stats', authenticate, async (req, res, next) => {
  try {
    // 1. Rooms statistics by status
    const roomStatusRes = await query(`
      SELECT status, COUNT(*)::int AS count 
      FROM rooms 
      GROUP BY status
    `);
    const roomsByStatus = {
      AVAILABLE: 0,
      OCCUPIED: 0,
      RESERVED: 0,
      MAINTENANCE: 0,
    };
    let totalRooms = 0;
    roomStatusRes.rows.forEach(r => {
      roomsByStatus[r.status] = r.count;
      totalRooms += r.count;
    });

    // 2. Room distribution by type
    const roomTypeRes = await query(`
      SELECT room_type, COUNT(*)::int AS count 
      FROM rooms 
      GROUP BY room_type
      ORDER BY count DESC
    `);
    const roomsByType = {};
    roomTypeRes.rows.forEach(r => {
      roomsByType[r.room_type] = r.count;
    });

    // 3. Total customers
    const custRes = await query('SELECT COUNT(*)::int AS count FROM customers');
    const totalCustomers = custRes.rows[0]?.count || 0;

    // 4. Active bookings
    const activeRes = await query(`
      SELECT COUNT(*)::int AS count 
      FROM bookings 
      WHERE booking_status IN ('RESERVED', 'CHECKED_IN')
    `);
    const activeBookings = activeRes.rows[0]?.count || 0;

    // 5. Today's Check-ins
    const checkInRes = await query(`
      SELECT COUNT(*)::int AS count 
      FROM bookings 
      WHERE check_in_date = CURRENT_DATE
    `);
    const todayCheckIns = checkInRes.rows[0]?.count || 0;

    // 6. Today's Check-outs
    const checkOutRes = await query(`
      SELECT COUNT(*)::int AS count 
      FROM bookings 
      WHERE actual_check_out_date = CURRENT_DATE 
         OR (booking_status = 'CHECKED_IN' AND expected_check_out_date = CURRENT_DATE)
    `);
    const todayCheckOuts = checkOutRes.rows[0]?.count || 0;

    // 7. Today's Revenue
    const todayRevRes = await query(`
      SELECT COALESCE(SUM(amount), 0)::numeric AS sum 
      FROM payments 
      WHERE payment_date::date = CURRENT_DATE AND payment_status = 'PAID'
    `);
    const todayRevenue = Number(todayRevRes.rows[0]?.sum || 0);

    // 8. Total Revenue
    const totalRevRes = await query(`
      SELECT COALESCE(SUM(amount), 0)::numeric AS sum 
      FROM payments 
      WHERE payment_status = 'PAID'
    `);
    const totalRevenue = Number(totalRevRes.rows[0]?.sum || 0);

    // 9. Recent Bookings (top 5 for dashboard table)
    const recentRes = await query(`
      SELECT b.booking_id, c.full_name AS customer_name, r.room_number, r.room_type,
             b.check_in_date, b.expected_check_out_date, b.booking_status,
             COALESCE(SUM(p.amount), 0)::numeric AS total_paid
      FROM bookings b
      JOIN customers c ON b.customer_id = c.customer_id
      JOIN rooms r ON b.room_id = r.room_id
      LEFT JOIN payments p ON b.booking_id = p.booking_id AND p.payment_status = 'PAID'
      GROUP BY b.booking_id, c.full_name, r.room_number, r.room_type, b.check_in_date, b.expected_check_out_date, b.booking_status
      ORDER BY b.booking_id DESC
      LIMIT 5
    `);

    res.json({
      success: true,
      stats: {
        totalRooms,
        availableRooms: roomsByStatus.AVAILABLE,
        occupiedRooms: roomsByStatus.OCCUPIED,
        reservedRooms: roomsByStatus.RESERVED,
        maintenanceRooms: roomsByStatus.MAINTENANCE,
        roomsByStatus,
        roomsByType,
        totalCustomers,
        activeBookings,
        todayCheckIns,
        todayCheckOuts,
        todayRevenue,
        totalRevenue,
      },
      recentBookings: recentRes.rows,
    });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
