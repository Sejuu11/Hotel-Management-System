package com.hotelmanagement.dao;

import com.hotelmanagement.database.DatabaseConnection;
import com.hotelmanagement.exception.DatabaseException;
import com.hotelmanagement.model.DashboardStats;
import com.hotelmanagement.util.DateUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object specialized for analytics, dashboard statistics,
 * aggregate queries, GROUP BY summaries, and multi-table JOIN reports.
 */
public class ReportDAO {

    /**
     * Retrieves live dynamic dashboard statistics using aggregate SQL queries.
     */
    public DashboardStats getDashboardStatistics() {
        DashboardStats stats = new DashboardStats();

        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Rooms statistics by status
            String sqlRoomStatus = "SELECT status, COUNT(*) AS count FROM rooms GROUP BY status";
            try (PreparedStatement ps = conn.prepareStatement(sqlRoomStatus);
                 ResultSet rs = ps.executeQuery()) {
                int total = 0;
                while (rs.next()) {
                    String st = rs.getString("status");
                    int cnt = rs.getInt("count");
                    total += cnt;
                    stats.getRoomsByStatus().put(st, cnt);
                    if ("AVAILABLE".equalsIgnoreCase(st)) stats.setAvailableRooms(cnt);
                    else if ("OCCUPIED".equalsIgnoreCase(st)) stats.setOccupiedRooms(cnt);
                    else if ("RESERVED".equalsIgnoreCase(st)) stats.setReservedRooms(cnt);
                    else if ("MAINTENANCE".equalsIgnoreCase(st)) stats.setMaintenanceRooms(cnt);
                }
                stats.setTotalRooms(total);
            }

            // 2. Room distribution by type
            String sqlRoomType = "SELECT room_type, COUNT(*) AS count FROM rooms GROUP BY room_type";
            try (PreparedStatement ps = conn.prepareStatement(sqlRoomType);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    stats.getRoomsByType().put(rs.getString("room_type"), rs.getInt("count"));
                }
            }

            // 3. Total customers
            String sqlCustomers = "SELECT COUNT(*) FROM customers";
            try (PreparedStatement ps = conn.prepareStatement(sqlCustomers);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.setTotalCustomers(rs.getInt(1));
            }

            // 4. Active bookings (RESERVED or CHECKED_IN)
            String sqlActiveBookings = "SELECT COUNT(*) FROM bookings WHERE booking_status IN ('RESERVED', 'CHECKED_IN')";
            try (PreparedStatement ps = conn.prepareStatement(sqlActiveBookings);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.setActiveBookings(rs.getInt(1));
            }

            // 5. Today's Check-ins
            String sqlTodayCheckIn = "SELECT COUNT(*) FROM bookings WHERE check_in_date = CURDATE()";
            try (PreparedStatement ps = conn.prepareStatement(sqlTodayCheckIn);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.setTodayCheckIns(rs.getInt(1));
            }

            // 6. Today's Check-outs
            String sqlTodayCheckOut = "SELECT COUNT(*) FROM bookings WHERE actual_check_out_date = CURDATE() " +
                                      "OR (booking_status = 'CHECKED_IN' AND expected_check_out_date = CURDATE())";
            try (PreparedStatement ps = conn.prepareStatement(sqlTodayCheckOut);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.setTodayCheckOuts(rs.getInt(1));
            }

            // 7. Today's Revenue
            String sqlTodayRev = "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE DATE(payment_date) = CURDATE() AND payment_status = 'PAID'";
            try (PreparedStatement ps = conn.prepareStatement(sqlTodayRev);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.setTodayRevenue(rs.getBigDecimal(1));
            }

            // 8. Total Revenue
            String sqlTotalRev = "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE payment_status = 'PAID'";
            try (PreparedStatement ps = conn.prepareStatement(sqlTotalRev);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.setTotalRevenue(rs.getBigDecimal(1));
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to load dashboard statistics: " + e.getMessage(), e);
        }

        return stats;
    }

    /**
     * Generates a detailed Booking Report using SQL JOINs across bookings, customers, rooms, and users.
     */
    public List<Object[]> getBookingReport(LocalDate fromDate, LocalDate toDate, String status) {
        List<Object[]> rows = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT b.booking_id, c.full_name, c.phone, r.room_number, r.room_type, " +
            "b.check_in_date, b.expected_check_out_date, b.actual_check_out_date, b.booking_status, " +
            "COALESCE(SUM(p.amount), 0) AS total_paid " +
            "FROM bookings b " +
            "JOIN customers c ON b.customer_id = c.customer_id " +
            "JOIN rooms r ON b.room_id = r.room_id " +
            "LEFT JOIN payments p ON b.booking_id = p.booking_id AND p.payment_status IN ('PAID', 'PARTIAL') " +
            "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (fromDate != null) {
            sql.append("AND b.check_in_date >= ? ");
            params.add(DateUtil.toSqlDate(fromDate));
        }
        if (toDate != null) {
            sql.append("AND b.check_in_date <= ? ");
            params.add(DateUtil.toSqlDate(toDate));
        }
        if (status != null && !status.equalsIgnoreCase("All")) {
            sql.append("AND b.booking_status = ? ");
            params.add(status);
        }
        sql.append("GROUP BY b.booking_id, c.full_name, c.phone, r.room_number, r.room_type, " +
                   "b.check_in_date, b.expected_check_out_date, b.actual_check_out_date, b.booking_status " +
                   "ORDER BY b.booking_id DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                        rs.getInt("booking_id"),
                        rs.getString("full_name"),
                        rs.getString("phone"),
                        rs.getString("room_number"),
                        rs.getString("room_type"),
                        rs.getDate("check_in_date"),
                        rs.getDate("expected_check_out_date"),
                        rs.getDate("actual_check_out_date"),
                        rs.getString("booking_status"),
                        "₹" + rs.getBigDecimal("total_paid")
                    });
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to generate booking report", e);
        }
        return rows;
    }

    /**
     * Generates Revenue Report with aggregate functions: COUNT(), SUM(), AVG(), MIN(), MAX().
     */
    public List<Object[]> getRevenueReport() {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT DATE(payment_date) AS txn_date, payment_method, " +
                     "COUNT(*) AS payment_count, " +
                     "SUM(amount) AS total_revenue, " +
                     "AVG(amount) AS avg_payment, " +
                     "MIN(amount) AS min_payment, " +
                     "MAX(amount) AS max_payment " +
                     "FROM payments " +
                     "WHERE payment_status = 'PAID' " +
                     "GROUP BY DATE(payment_date), payment_method " +
                     "ORDER BY txn_date DESC, total_revenue DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[]{
                    rs.getDate("txn_date"),
                    rs.getString("payment_method"),
                    rs.getInt("payment_count"),
                    "₹" + rs.getBigDecimal("total_revenue"),
                    "₹" + rs.getBigDecimal("avg_payment").setScale(2, java.math.RoundingMode.HALF_UP),
                    "₹" + rs.getBigDecimal("min_payment"),
                    "₹" + rs.getBigDecimal("max_payment")
                });
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to generate revenue report", e);
        }
        return rows;
    }

    /**
     * Generates Room Utilization Report with GROUP BY room_type, aggregate counts, and AVG/MIN/MAX price.
     */
    public List<Object[]> getRoomReport() {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT room_type, " +
                     "COUNT(*) AS total_rooms, " +
                     "SUM(CASE WHEN status = 'AVAILABLE' THEN 1 ELSE 0 END) AS available_count, " +
                     "SUM(CASE WHEN status = 'OCCUPIED' THEN 1 ELSE 0 END) AS occupied_count, " +
                     "SUM(CASE WHEN status = 'RESERVED' THEN 1 ELSE 0 END) AS reserved_count, " +
                     "SUM(CASE WHEN status = 'MAINTENANCE' THEN 1 ELSE 0 END) AS maintenance_count, " +
                     "AVG(price_per_night) AS avg_price, " +
                     "MIN(price_per_night) AS min_price, " +
                     "MAX(price_per_night) AS max_price " +
                     "FROM rooms " +
                     "GROUP BY room_type " +
                     "ORDER BY total_rooms DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[]{
                    rs.getString("room_type"),
                    rs.getInt("total_rooms"),
                    rs.getInt("available_count"),
                    rs.getInt("occupied_count"),
                    rs.getInt("reserved_count"),
                    rs.getInt("maintenance_count"),
                    "₹" + rs.getBigDecimal("avg_price").setScale(2, java.math.RoundingMode.HALF_UP),
                    "₹" + rs.getBigDecimal("min_price"),
                    "₹" + rs.getBigDecimal("max_price")
                });
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to generate room report", e);
        }
        return rows;
    }

    /**
     * Generates Employee Payroll and Department Report with GROUP BY designation and SUM/AVG salary.
     */
    public List<Object[]> getEmployeeReport() {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT designation, " +
                     "COUNT(*) AS staff_count, " +
                     "SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) AS active_count, " +
                     "SUM(salary) AS total_payroll, " +
                     "AVG(salary) AS avg_salary, " +
                     "MIN(salary) AS min_salary, " +
                     "MAX(salary) AS max_salary " +
                     "FROM employees " +
                     "GROUP BY designation " +
                     "ORDER BY total_payroll DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[]{
                    rs.getString("designation"),
                    rs.getInt("staff_count"),
                    rs.getInt("active_count"),
                    "₹" + rs.getBigDecimal("total_payroll"),
                    "₹" + rs.getBigDecimal("avg_salary").setScale(2, java.math.RoundingMode.HALF_UP),
                    "₹" + rs.getBigDecimal("min_salary"),
                    "₹" + rs.getBigDecimal("max_salary")
                });
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to generate employee report", e);
        }
        return rows;
    }
}
