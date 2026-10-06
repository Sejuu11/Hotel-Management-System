package com.hotelmanagement.service;

import com.hotelmanagement.dao.BookingDAO;
import com.hotelmanagement.dao.PaymentDAO;
import com.hotelmanagement.dao.RoomDAO;
import com.hotelmanagement.database.DatabaseConnection;
import com.hotelmanagement.exception.DatabaseException;
import com.hotelmanagement.exception.ValidationException;
import com.hotelmanagement.model.BillDetails;
import com.hotelmanagement.model.Booking;
import com.hotelmanagement.model.Payment;
import com.hotelmanagement.util.DateUtil;
import com.hotelmanagement.util.ValidationUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Service managing guest check-in, dynamic invoice billing, and atomic transactional check-out.
 */
public class CheckInOutService {
    private final BookingDAO bookingDAO;
    private final RoomDAO roomDAO;
    private final PaymentDAO paymentDAO;

    public CheckInOutService() {
        this.bookingDAO = new BookingDAO();
        this.roomDAO = new RoomDAO();
        this.paymentDAO = new PaymentDAO();
    }

    public CheckInOutService(BookingDAO bookingDAO, RoomDAO roomDAO, PaymentDAO paymentDAO) {
        this.bookingDAO = bookingDAO;
        this.roomDAO = roomDAO;
        this.paymentDAO = paymentDAO;
    }

    /**
     * Executes check-in for a reserved booking.
     * Transitions: Booking RESERVED -> CHECKED_IN, Room RESERVED -> OCCUPIED.
     */
    public boolean checkIn(int bookingId) {
        Booking booking = bookingDAO.findById(bookingId);
        if (booking == null) {
            throw new ValidationException("Booking #" + bookingId + " not found.");
        }
        if (!"RESERVED".equalsIgnoreCase(booking.getBookingStatus())) {
            throw new ValidationException("Only bookings with status 'RESERVED' can be checked in. Current status: " + booking.getBookingStatus());
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Update booking status to CHECKED_IN
            boolean bUpdated = bookingDAO.updateStatus(bookingId, "CHECKED_IN", null, conn);
            // Update room status to OCCUPIED
            boolean rUpdated = roomDAO.updateStatus(booking.getRoomId(), "OCCUPIED", conn);

            if (bUpdated && rUpdated) {
                conn.commit();
                return true;
            } else {
                conn.rollback();
                throw new DatabaseException("Failed to update booking or room status during check-in.");
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback(conn);
            throw new DatabaseException("Database error during check-in: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(conn);
        }
    }

    /**
     * Calculates the detailed bill for checkout based on stay duration, room rate,
     * additional charges, taxes, and prior payments.
     */
    public BillDetails calculateBill(int bookingId, BigDecimal additionalCharges, BigDecimal taxRate) {
        Booking booking = bookingDAO.findById(bookingId);
        if (booking == null) {
            throw new ValidationException("Booking #" + bookingId + " not found.");
        }

        LocalDate today = LocalDate.now();
        LocalDate checkIn = booking.getCheckInDate() != null ? booking.getCheckInDate() : today;
        LocalDate checkOut = today;
        // If checking out on or before check-in, count as 1 night
        long nights = DateUtil.calculateNights(checkIn, checkOut);

        BigDecimal pricePerNight = booking.getRoomPricePerNight() != null ? booking.getRoomPricePerNight() : BigDecimal.ZERO;
        BigDecimal alreadyPaid = paymentDAO.getTotalPaidForBooking(bookingId);

        BillDetails bill = new BillDetails();
        bill.setBookingId(booking.getBookingId());
        bill.setCustomerId(booking.getCustomerId());
        bill.setCustomerName(booking.getCustomerName());
        bill.setCustomerPhone(booking.getCustomerPhone());
        bill.setRoomId(booking.getRoomId());
        bill.setRoomNumber(booking.getRoomNumber());
        bill.setRoomType(booking.getRoomType());
        bill.setCheckInDate(checkIn);
        bill.setCheckOutDate(checkOut);
        bill.setNumberOfNights(nights);
        bill.setPricePerNight(pricePerNight);
        bill.setAdditionalCharges(additionalCharges != null ? additionalCharges : BigDecimal.ZERO);
        bill.setTaxRate(taxRate != null ? taxRate : new BigDecimal("0.12"));
        bill.setPaidAmount(alreadyPaid);

        bill.recalculate();
        return bill;
    }

    /**
     * Executes atomic transactional check-out:
     * 1. Records payment if applicable.
     * 2. Updates booking status to CHECKED_OUT with actual checkout date.
     * 3. Updates room status to AVAILABLE.
     * All operations succeed or rollback together in a single JDBC transaction.
     */
    public boolean checkOut(int bookingId, BigDecimal additionalCharges, BigDecimal taxRate,
                            BigDecimal paymentAmount, String paymentMethod, String txnRef) {
        Booking booking = bookingDAO.findById(bookingId);
        if (booking == null) {
            throw new ValidationException("Booking #" + bookingId + " not found.");
        }
        if (!"CHECKED_IN".equalsIgnoreCase(booking.getBookingStatus())) {
            throw new ValidationException("Only 'CHECKED_IN' bookings can be checked out. Current status: " + booking.getBookingStatus());
        }

        BillDetails bill = calculateBill(bookingId, additionalCharges, taxRate);

        if (paymentAmount != null && paymentAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Validate payment amount does not exceed balance
            if (paymentAmount.compareTo(bill.getBalanceDue()) > 0) {
                throw new ValidationException("Payment amount (₹" + paymentAmount + ") cannot exceed remaining balance (₹" + bill.getBalanceDue() + ").");
            }
            ValidationUtil.requireNonEmpty(paymentMethod, "Payment method");
        }

        // Remaining balance after this payment
        BigDecimal afterPaymentBalance = bill.getBalanceDue().subtract(paymentAmount != null ? paymentAmount : BigDecimal.ZERO);
        if (afterPaymentBalance.compareTo(new BigDecimal("0.01")) > 0) {
            throw new ValidationException("Outstanding balance of ₹" + afterPaymentBalance + " must be settled prior to completing checkout.");
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Begin JDBC transaction

            // Step 1: Record settlement payment if amount provided
            if (paymentAmount != null && paymentAmount.compareTo(BigDecimal.ZERO) > 0) {
                Payment settlementPayment = new Payment();
                settlementPayment.setBookingId(bookingId);
                settlementPayment.setCustomerId(booking.getCustomerId());
                settlementPayment.setAmount(paymentAmount);
                settlementPayment.setPaymentMethod(paymentMethod);
                settlementPayment.setPaymentStatus("PAID");
                settlementPayment.setTransactionReference(txnRef != null && !txnRef.trim().isEmpty() ? txnRef.trim() : "CHECKOUT-SETTLE-" + System.currentTimeMillis());

                boolean paymentSuccess = paymentDAO.create(settlementPayment, conn);
                if (!paymentSuccess) {
                    conn.rollback();
                    throw new DatabaseException("Failed to record settlement payment. Checkout rolled back.");
                }
            }

            // Step 2: Update booking status to CHECKED_OUT with actual check-out date
            LocalDate actualCheckOut = LocalDate.now();
            boolean bookingSuccess = bookingDAO.updateStatus(bookingId, "CHECKED_OUT", actualCheckOut, conn);
            if (!bookingSuccess) {
                conn.rollback();
                throw new DatabaseException("Failed to update booking status to CHECKED_OUT. Checkout rolled back.");
            }

            // Step 3: Release room back to AVAILABLE
            boolean roomSuccess = roomDAO.updateStatus(booking.getRoomId(), "AVAILABLE", conn);
            if (!roomSuccess) {
                conn.rollback();
                throw new DatabaseException("Failed to release room back to AVAILABLE. Checkout rolled back.");
            }

            // Commit atomic transaction
            conn.commit();
            return true;
        } catch (SQLException e) {
            DatabaseConnection.rollback(conn);
            throw new DatabaseException("Transaction failed during checkout: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(conn);
        }
    }
}
