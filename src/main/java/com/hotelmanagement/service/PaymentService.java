package com.hotelmanagement.service;

import com.hotelmanagement.dao.BookingDAO;
import com.hotelmanagement.dao.PaymentDAO;
import com.hotelmanagement.exception.ValidationException;
import com.hotelmanagement.model.Booking;
import com.hotelmanagement.model.Payment;
import com.hotelmanagement.util.DateUtil;
import com.hotelmanagement.util.ValidationUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Service managing payment transactions, outstanding balance checks, and payment history.
 */
public class PaymentService {
    private final PaymentDAO paymentDAO;
    private final BookingDAO bookingDAO;

    public PaymentService() {
        this.paymentDAO = new PaymentDAO();
        this.bookingDAO = new BookingDAO();
    }

    public PaymentService(PaymentDAO paymentDAO, BookingDAO bookingDAO) {
        this.paymentDAO = paymentDAO;
        this.bookingDAO = bookingDAO;
    }

    public boolean recordPayment(Payment payment) {
        validatePayment(payment);

        Booking booking = bookingDAO.findById(payment.getBookingId());
        if (booking == null) {
            throw new ValidationException("Booking #" + payment.getBookingId() + " does not exist.");
        }

        // Calculate maximum allowable payment based on estimated total
        long nights = DateUtil.calculateNights(booking.getCheckInDate(), booking.getExpectedCheckOutDate());
        BigDecimal roomRate = booking.getRoomPricePerNight() != null ? booking.getRoomPricePerNight() : BigDecimal.ZERO;
        BigDecimal estimatedTotal = roomRate.multiply(BigDecimal.valueOf(nights)).multiply(new BigDecimal("1.12")).setScale(2, RoundingMode.HALF_UP);

        BigDecimal existingPaid = paymentDAO.getTotalPaidForBooking(payment.getBookingId());
        BigDecimal remaining = estimatedTotal.subtract(existingPaid);

        if (payment.getAmount().compareTo(remaining) > 0) {
            throw new ValidationException("Payment amount (₹" + payment.getAmount() + ") exceeds estimated outstanding balance of ₹" + remaining);
        }

        if (payment.getCustomerId() <= 0) {
            payment.setCustomerId(booking.getCustomerId());
        }

        return paymentDAO.create(payment);
    }

    public List<Payment> getPaymentsForBooking(int bookingId) {
        return paymentDAO.findByBookingId(bookingId);
    }

    public BigDecimal getTotalPaidForBooking(int bookingId) {
        return paymentDAO.getTotalPaidForBooking(bookingId);
    }

    public List<Payment> getAllPayments() {
        return paymentDAO.findAllWithDetails();
    }

    public List<Payment> searchPayments(String keyword, String method, String status) {
        return paymentDAO.search(keyword, method, status);
    }

    private void validatePayment(Payment p) {
        if (p.getBookingId() <= 0) {
            throw new ValidationException("Booking ID is required.");
        }
        ValidationUtil.validatePositiveNumber(p.getAmount(), "Payment amount");
        ValidationUtil.requireNonEmpty(p.getPaymentMethod(), "Payment method");
    }
}
