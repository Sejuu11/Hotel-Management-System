package com.hotelmanagement.dao;

import com.hotelmanagement.database.DatabaseConnection;
import com.hotelmanagement.exception.DatabaseException;
import com.hotelmanagement.model.Room;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for rooms table with transaction-aware status updates.
 */
public class RoomDAO {

    public boolean create(Room room) {
        String sql = "INSERT INTO rooms (room_number, room_type, floor, price_per_night, status, description) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, room.getRoomNumber());
            ps.setString(2, room.getRoomType());
            ps.setInt(3, room.getFloor());
            ps.setBigDecimal(4, room.getPricePerNight());
            ps.setString(5, room.getStatus());
            ps.setString(6, room.getDescription());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        room.setRoomId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert room: " + e.getMessage(), e);
        }
        return false;
    }

    public boolean update(Room room) {
        String sql = "UPDATE rooms SET room_number = ?, room_type = ?, floor = ?, " +
                     "price_per_night = ?, status = ?, description = ? WHERE room_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, room.getRoomNumber());
            ps.setString(2, room.getRoomType());
            ps.setInt(3, room.getFloor());
            ps.setBigDecimal(4, room.getPricePerNight());
            ps.setString(5, room.getStatus());
            ps.setString(6, room.getDescription());
            ps.setInt(7, room.getRoomId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update room: " + e.getMessage(), e);
        }
    }

    public boolean delete(int roomId) {
        String sql = "DELETE FROM rooms WHERE room_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Cannot delete room. Active or past bookings are associated with this room.", e);
        }
    }

    public Room findById(int roomId) {
        String sql = "SELECT * FROM rooms WHERE room_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find room by ID: " + roomId, e);
        }
        return null;
    }

    public Room findByRoomNumber(String roomNumber) {
        String sql = "SELECT * FROM rooms WHERE room_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find room by number: " + roomNumber, e);
        }
        return null;
    }

    public List<Room> findAll() {
        List<Room> list = new ArrayList<>();
        String sql = "SELECT * FROM rooms ORDER BY floor ASC, room_number ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve rooms", e);
        }
        return list;
    }

    public List<Room> findAvailableRooms() {
        List<Room> list = new ArrayList<>();
        String sql = "SELECT * FROM rooms WHERE status = 'AVAILABLE' ORDER BY floor ASC, room_number ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve available rooms", e);
        }
        return list;
    }

    /**
     * Updates room status. Overloaded method to support transactional updates.
     */
    public boolean updateStatus(int roomId, String status) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return updateStatus(roomId, status, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update room status: " + e.getMessage(), e);
        }
    }

    public boolean updateStatus(int roomId, String status, Connection conn) throws SQLException {
        String sql = "UPDATE rooms SET status = ? WHERE room_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, roomId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean hasActiveBookings(int roomId) {
        String sql = "SELECT COUNT(*) FROM bookings WHERE room_id = ? AND booking_status IN ('RESERVED', 'CHECKED_IN')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check room active bookings", e);
        }
        return false;
    }

    public List<Room> search(String keyword, String roomType, String status) {
        List<Room> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM rooms WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (room_number LIKE ? OR description LIKE ?) ");
            String pat = "%" + keyword.trim() + "%";
            params.add(pat);
            params.add(pat);
        }
        if (roomType != null && !roomType.equalsIgnoreCase("All")) {
            sql.append("AND room_type = ? ");
            params.add(roomType);
        }
        if (status != null && !status.equalsIgnoreCase("All")) {
            sql.append("AND status = ? ");
            params.add(status);
        }
        sql.append("ORDER BY floor ASC, room_number ASC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Room search query failed", e);
        }
        return list;
    }

    private Room mapRow(ResultSet rs) throws SQLException {
        return new Room(
            rs.getInt("room_id"),
            rs.getString("room_number"),
            rs.getString("room_type"),
            rs.getInt("floor"),
            rs.getBigDecimal("price_per_night"),
            rs.getString("status"),
            rs.getString("description")
        );
    }
}
