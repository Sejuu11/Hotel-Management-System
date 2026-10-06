package com.hotelmanagement.service;

import com.hotelmanagement.dao.RoomDAO;
import com.hotelmanagement.exception.ValidationException;
import com.hotelmanagement.model.Room;
import com.hotelmanagement.util.ValidationUtil;

import java.util.List;

/**
 * Service managing room inventory, status updates, and constraint validations.
 */
public class RoomService {
    private final RoomDAO roomDAO;

    public RoomService() {
        this.roomDAO = new RoomDAO();
    }

    public RoomService(RoomDAO roomDAO) {
        this.roomDAO = roomDAO;
    }

    public boolean addRoom(Room room) {
        validateRoom(room);
        // Verify duplicate room number
        Room existing = roomDAO.findByRoomNumber(room.getRoomNumber().trim());
        if (existing != null) {
            throw new ValidationException("Room number '" + room.getRoomNumber() + "' already exists.");
        }
        return roomDAO.create(room);
    }

    public boolean updateRoom(Room room) {
        if (room.getRoomId() <= 0) {
            throw new ValidationException("Room ID is required for update.");
        }
        validateRoom(room);
        Room existing = roomDAO.findByRoomNumber(room.getRoomNumber().trim());
        if (existing != null && existing.getRoomId() != room.getRoomId()) {
            throw new ValidationException("Room number '" + room.getRoomNumber() + "' is already assigned to another room.");
        }
        return roomDAO.update(room);
    }

    public boolean deleteRoom(int roomId) {
        if (roomId <= 0) {
            throw new ValidationException("Invalid room ID.");
        }
        if (roomDAO.hasActiveBookings(roomId)) {
            throw new ValidationException("Cannot delete room: Room currently has active (Reserved / Occupied) bookings.");
        }
        return roomDAO.delete(roomId);
    }

    public boolean updateRoomStatus(int roomId, String status) {
        if (roomId <= 0) {
            throw new ValidationException("Invalid room ID.");
        }
        ValidationUtil.requireNonEmpty(status, "Status");
        return roomDAO.updateStatus(roomId, status);
    }

    public Room getRoomById(int roomId) {
        return roomDAO.findById(roomId);
    }

    public List<Room> getAllRooms() {
        return roomDAO.findAll();
    }

    public List<Room> getAvailableRooms() {
        return roomDAO.findAvailableRooms();
    }

    public List<Room> searchRooms(String keyword, String type, String status) {
        return roomDAO.search(keyword, type, status);
    }

    private void validateRoom(Room r) {
        ValidationUtil.requireNonEmpty(r.getRoomNumber(), "Room Number");
        ValidationUtil.requireNonEmpty(r.getRoomType(), "Room Type");
        ValidationUtil.validatePositiveInteger(r.getFloor(), "Floor");
        ValidationUtil.validatePositiveNumber(r.getPricePerNight(), "Price Per Night");
        ValidationUtil.requireNonEmpty(r.getStatus(), "Status");
    }
}
