package com.hotelmanagement.service;

import com.hotelmanagement.dao.BookingDAO;
import com.hotelmanagement.dao.CustomerDAO;
import com.hotelmanagement.dao.RoomDAO;
import com.hotelmanagement.database.DatabaseConnection;
import com.hotelmanagement.exception.DatabaseException;
import com.hotelmanagement.exception.ValidationException;
import com.hotelmanagement.model.Booking;
import com.hotelmanagement.model.Customer;
import com.hotelmanagement.model.Room;
import com.hotelmanagement.util.DateUtil;
import com.hotelmanagement.util.ValidationUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Service managing room reservations, date collision checks, and reservation lifecycle.
 */
public class BookingService {
    private final BookingDAO bookingDAO;
    private final RoomDAO roomDAO;
    private final CustomerDAO customerDAO;

    public BookingService() {
        this.bookingDAO = new BookingDAO();
        this.roomDAO = new RoomDAO();
        this.customerDAO = new CustomerDAO();
    }

    public BookingService(BookingDAO bookingDAO, RoomDAO roomDAO, CustomerDAO customerDAO) {
        this.bookingDAO = bookingDAO;
        this.roomDAO = roomDAO;
        this.customerDAO = customerDAO;
    }

    public boolean createBooking(Booking booking) {
        validateBooking(booking);

        // 1. Verify customer exists
        Customer customer = customerDAO.findById(booking.getCustomerId());
        if (customer == null) {
            throw new ValidationException("Customer with ID " + booking.getCustomerId() + " does not exist.");
        }

        // 2. Verify room exists and is currently available
        Room room = roomDAO.findById(booking.getRoomId());
        if (room == null) {
            throw new ValidationException("Room with ID " + booking.getRoomId() + " does not exist.");
        }
        if (!"AVAILABLE".equalsIgnoreCase(room.getStatus())) {
            throw new ValidationException("Room " + room.getRoomNumber() + " is currently " + room.getStatus() + " and cannot be reserved.");
        }

        // 3. Verify no date overlap conflict
        if (bookingDAO.isRoomBookedForDates(booking.getRoomId(), booking.getCheckInDate(), booking.getExpectedCheckOutDate(), null)) {
            throw new ValidationException("Room " + room.getRoomNumber() + " is already booked for the specified dates.");
        }

        booking.setBookingDate(LocalDate.now());
        booking.setBookingStatus("RESERVED");

        // Transactional insertion and room status update
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            boolean bookingCreated = bookingDAO.create(booking, conn);
            if (!bookingCreated) {
                conn.rollback();
                return false;
            }

            // Update room to RESERVED
            boolean roomUpdated = roomDAO.updateStatus(booking.getRoomId(), "RESERVED", conn);
            if (!roomUpdated) {
                conn.rollback();
                return false;
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            DatabaseConnection.rollback(conn);
            throw new DatabaseException("Failed to reserve room: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(conn);
        }
    }

    public boolean cancelBooking(int bookingId) {
        Booking booking = bookingDAO.findById(bookingId);
        if (booking == null) {
            throw new ValidationException("Booking not found.");
        }
        if ("CHECKED_OUT".equalsIgnoreCase(booking.getBookingStatus())) {
            throw new ValidationException("Cannot cancel an already completed / checked-out booking.");
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            bookingDAO.updateStatus(bookingId, "CANCELLED", LocalDate.now(), conn);
            roomDAO.updateStatus(booking.getRoomId(), "AVAILABLE", conn);

            conn.commit();
            return true;
        } catch (SQLException e) {
            DatabaseConnection.rollback(conn);
            throw new DatabaseException("Failed to cancel booking: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(conn);
        }
    }

    public List<Booking> getAllBookings() {
        return bookingDAO.findAllWithDetails();
    }

    public List<Booking> getBookingsByStatus(String status) {
        return bookingDAO.findBookingsByStatus(status);
    }

    public Booking getBookingById(int bookingId) {
        return bookingDAO.findById(bookingId);
    }

    public List<Booking> searchBookings(String keyword, String status) {
        return bookingDAO.search(keyword, status);
    }

    private void validateBooking(Booking b) {
        if (b.getCustomerId() <= 0) {
            throw new ValidationException("Please select a valid customer.");
        }
        if (b.getRoomId() <= 0) {
            throw new ValidationException("Please select a room.");
        }
        ValidationUtil.validateBookingDates(b.getCheckInDate(), b.getExpectedCheckOutDate());
        ValidationUtil.validatePositiveInteger(b.getNumberOfGuests(), "Number of guests");
    }
}
