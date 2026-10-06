package com.hotelmanagement.dao;

import com.hotelmanagement.database.DatabaseConnection;
import com.hotelmanagement.exception.DatabaseException;
import com.hotelmanagement.model.Booking;
import com.hotelmanagement.util.DateUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for bookings with relational JOIN queries and transaction support.
 */
public class BookingDAO {

    public boolean create(Booking booking) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return create(booking, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to create booking: " + e.getMessage(), e);
        }
    }

    public boolean create(Booking booking, Connection conn) throws SQLException {
        String sql = "INSERT INTO bookings (customer_id, room_id, booking_date, check_in_date, " +
                     "expected_check_out_date, number_of_guests, booking_status, special_request, created_by) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, booking.getCustomerId());
            ps.setInt(2, booking.getRoomId());
            ps.setDate(3, DateUtil.toSqlDate(booking.getBookingDate()));
            ps.setDate(4, DateUtil.toSqlDate(booking.getCheckInDate()));
            ps.setDate(5, DateUtil.toSqlDate(booking.getExpectedCheckOutDate()));
            ps.setInt(6, booking.getNumberOfGuests());
            ps.setString(7, booking.getBookingStatus() != null ? booking.getBookingStatus() : "RESERVED");
            ps.setString(8, booking.getSpecialRequest());
            if (booking.getCreatedBy() != null) {
                ps.setInt(9, booking.getCreatedBy());
            } else {
                ps.setNull(9, Types.INTEGER);
            }

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        booking.setBookingId(rs.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean update(Booking booking) {
        String sql = "UPDATE bookings SET customer_id = ?, room_id = ?, check_in_date = ?, " +
                     "expected_check_out_date = ?, actual_check_out_date = ?, number_of_guests = ?, " +
                     "booking_status = ?, special_request = ? WHERE booking_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, booking.getCustomerId());
            ps.setInt(2, booking.getRoomId());
            ps.setDate(3, DateUtil.toSqlDate(booking.getCheckInDate()));
            ps.setDate(4, DateUtil.toSqlDate(booking.getExpectedCheckOutDate()));
            ps.setDate(5, DateUtil.toSqlDate(booking.getActualCheckOutDate()));
            ps.setInt(6, booking.getNumberOfGuests());
            ps.setString(7, booking.getBookingStatus());
            ps.setString(8, booking.getSpecialRequest());
            ps.setInt(9, booking.getBookingId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update booking: " + e.getMessage(), e);
        }
    }

    public boolean updateStatus(int bookingId, String status, LocalDate actualCheckOutDate, Connection conn) throws SQLException {
        String sql = "UPDATE bookings SET booking_status = ?, actual_check_out_date = ? WHERE booking_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setDate(2, DateUtil.toSqlDate(actualCheckOutDate));
            ps.setInt(3, bookingId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateStatus(int bookingId, String status, LocalDate actualCheckOutDate) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return updateStatus(bookingId, status, actualCheckOutDate, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update booking status", e);
        }
    }

    public boolean delete(int bookingId) {
        String sql = "DELETE FROM bookings WHERE booking_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete booking: " + e.getMessage(), e);
        }
    }

    public Booking findById(int bookingId) {
        String sql = "SELECT b.*, c.full_name AS customer_name, c.phone AS customer_phone, " +
                     "r.room_number, r.room_type, r.price_per_night " +
                     "FROM bookings b " +
                     "JOIN customers c ON b.customer_id = c.customer_id " +
                     "JOIN rooms r ON b.room_id = r.room_id " +
                     "WHERE b.booking_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowWithDetails(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find booking by ID: " + bookingId, e);
        }
        return null;
    }

    public List<Booking> findAllWithDetails() {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.*, c.full_name AS customer_name, c.phone AS customer_phone, " +
                     "r.room_number, r.room_type, r.price_per_night " +
                     "FROM bookings b " +
                     "JOIN customers c ON b.customer_id = c.customer_id " +
                     "JOIN rooms r ON b.room_id = r.room_id " +
                     "ORDER BY b.booking_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowWithDetails(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve bookings", e);
        }
        return list;
    }

    public List<Booking> findBookingsByStatus(String status) {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.*, c.full_name AS customer_name, c.phone AS customer_phone, " +
                     "r.room_number, r.room_type, r.price_per_night " +
                     "FROM bookings b " +
                     "JOIN customers c ON b.customer_id = c.customer_id " +
                     "JOIN rooms r ON b.room_id = r.room_id " +
                     "WHERE b.booking_status = ? " +
                     "ORDER BY b.booking_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowWithDetails(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve bookings by status: " + status, e);
        }
        return list;
    }

    public List<Booking> search(String keyword, String status) {
        List<Booking> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT b.*, c.full_name AS customer_name, c.phone AS customer_phone, " +
            "r.room_number, r.room_type, r.price_per_night " +
            "FROM bookings b " +
            "JOIN customers c ON b.customer_id = c.customer_id " +
            "JOIN rooms r ON b.room_id = r.room_id WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (c.full_name LIKE ? OR c.phone LIKE ? OR r.room_number LIKE ? OR CAST(b.booking_id AS CHAR) = ?) ");
            String pat = "%" + keyword.trim() + "%";
            params.add(pat);
            params.add(pat);
            params.add(pat);
            params.add(keyword.trim());
        }
        if (status != null && !status.equalsIgnoreCase("All")) {
            sql.append("AND b.booking_status = ? ");
            params.add(status);
        }
        sql.append("ORDER BY b.booking_id DESC");

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
            throw new DatabaseException("Booking search query failed", e);
        }
        return list;
    }

    /**
     * Checks if a room is already reserved or occupied for overlapping dates.
     */
    public boolean isRoomBookedForDates(int roomId, LocalDate checkIn, LocalDate checkOut, Integer excludeBookingId) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) FROM bookings " +
            "WHERE room_id = ? AND booking_status IN ('RESERVED', 'CHECKED_IN') " +
            "AND NOT (expected_check_out_date <= ? OR check_in_date >= ?) "
        );
        if (excludeBookingId != null) {
            sql.append("AND booking_id <> ?");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, roomId);
            ps.setDate(2, DateUtil.toSqlDate(checkIn));
            ps.setDate(3, DateUtil.toSqlDate(checkOut));
            if (excludeBookingId != null) {
                ps.setInt(4, excludeBookingId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check room availability dates", e);
        }
        return false;
    }

    private Booking mapRowWithDetails(ResultSet rs) throws SQLException {
        Date bDate = rs.getDate("booking_date");
        Date ciDate = rs.getDate("check_in_date");
        Date ecoDate = rs.getDate("expected_check_out_date");
        Date acoDate = rs.getDate("actual_check_out_date");

        Booking b = new Booking(
            rs.getInt("booking_id"),
            rs.getInt("customer_id"),
            rs.getInt("room_id"),
            bDate != null ? bDate.toLocalDate() : null,
            ciDate != null ? ciDate.toLocalDate() : null,
            ecoDate != null ? ecoDate.toLocalDate() : null,
            acoDate != null ? acoDate.toLocalDate() : null,
            rs.getInt("number_of_guests"),
            rs.getString("booking_status"),
            rs.getString("special_request"),
            rs.getObject("created_by") != null ? rs.getInt("created_by") : null
        );

        b.setCustomerName(rs.getString("customer_name"));
        b.setCustomerPhone(rs.getString("customer_phone"));
        b.setRoomNumber(rs.getString("room_number"));
        b.setRoomType(rs.getString("room_type"));
        b.setRoomPricePerNight(rs.getBigDecimal("price_per_night"));

        return b;
    }
}
