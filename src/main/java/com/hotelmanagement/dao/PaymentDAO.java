package com.hotelmanagement.dao;

import com.hotelmanagement.database.DatabaseConnection;
import com.hotelmanagement.exception.DatabaseException;
import com.hotelmanagement.model.Payment;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for payments table with transaction-aware insert.
 */
public class PaymentDAO {

    public boolean create(Payment payment) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return create(payment, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to record payment: " + e.getMessage(), e);
        }
    }

    public boolean create(Payment payment, Connection conn) throws SQLException {
        String sql = "INSERT INTO payments (booking_id, customer_id, amount, payment_date, payment_method, " +
                     "payment_status, transaction_reference) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, payment.getBookingId());
            ps.setInt(2, payment.getCustomerId());
            ps.setBigDecimal(3, payment.getAmount());
            if (payment.getPaymentDate() != null) {
                ps.setTimestamp(4, payment.getPaymentDate());
            } else {
                ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            }
            ps.setString(5, payment.getPaymentMethod());
            ps.setString(6, payment.getPaymentStatus() != null ? payment.getPaymentStatus() : "PAID");
            ps.setString(7, payment.getTransactionReference());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        payment.setPaymentId(rs.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public BigDecimal getTotalPaidForBooking(int bookingId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE booking_id = ? AND payment_status IN ('PAID', 'PARTIAL')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to calculate total paid for booking: " + bookingId, e);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getTotalPaidForBooking(int bookingId, Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE booking_id = ? AND payment_status IN ('PAID', 'PARTIAL')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
            }
        }
        return BigDecimal.ZERO;
    }

    public List<Payment> findByBookingId(int bookingId) {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT p.*, c.full_name AS customer_name, r.room_number " +
                     "FROM payments p " +
                     "JOIN customers c ON p.customer_id = c.customer_id " +
                     "JOIN bookings b ON p.booking_id = b.booking_id " +
                     "JOIN rooms r ON b.room_id = r.room_id " +
                     "WHERE p.booking_id = ? ORDER BY p.payment_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowWithDetails(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve payments for booking: " + bookingId, e);
        }
        return list;
    }

    public List<Payment> findAllWithDetails() {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT p.*, c.full_name AS customer_name, r.room_number " +
                     "FROM payments p " +
                     "JOIN customers c ON p.customer_id = c.customer_id " +
                     "JOIN bookings b ON p.booking_id = b.booking_id " +
                     "JOIN rooms r ON b.room_id = r.room_id " +
                     "ORDER BY p.payment_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowWithDetails(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve payments", e);
        }
        return list;
    }

    public List<Payment> search(String keyword, String method, String status) {
        List<Payment> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT p.*, c.full_name AS customer_name, r.room_number " +
            "FROM payments p " +
            "JOIN customers c ON p.customer_id = c.customer_id " +
            "JOIN bookings b ON p.booking_id = b.booking_id " +
            "JOIN rooms r ON b.room_id = r.room_id WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (c.full_name LIKE ? OR r.room_number LIKE ? OR p.transaction_reference LIKE ? OR CAST(p.booking_id AS CHAR) = ?) ");
            String pat = "%" + keyword.trim() + "%";
            params.add(pat);
            params.add(pat);
            params.add(pat);
            params.add(keyword.trim());
        }
        if (method != null && !method.equalsIgnoreCase("All")) {
            sql.append("AND p.payment_method = ? ");
            params.add(method);
        }
        if (status != null && !status.equalsIgnoreCase("All")) {
            sql.append("AND p.payment_status = ? ");
            params.add(status);
        }
        sql.append("ORDER BY p.payment_id DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowWithDetails(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Payment search query failed", e);
        }
        return list;
    }

    private Payment mapRowWithDetails(ResultSet rs) throws SQLException {
        Payment p = new Payment(
            rs.getInt("payment_id"),
            rs.getInt("booking_id"),
            rs.getInt("customer_id"),
            rs.getBigDecimal("amount"),
            rs.getTimestamp("payment_date"),
            rs.getString("payment_method"),
            rs.getString("payment_status"),
            rs.getString("transaction_reference")
        );
        p.setCustomerName(rs.getString("customer_name"));
        p.setRoomNumber(rs.getString("room_number"));
        return p;
    }
}
