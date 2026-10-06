package com.hotelmanagement.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Represents a payment transaction made by a customer for a booking.
 */
public class Payment {
    private int paymentId;
    private int bookingId;
    private int customerId;
    private BigDecimal amount;
    private Timestamp paymentDate;
    private String paymentMethod; // CASH, CARD, UPI, BANK_TRANSFER
    private String paymentStatus; // PAID, PARTIAL, REFUNDED, PENDING
    private String transactionReference;

    // View helper fields
    private String customerName;
    private String roomNumber;

    public Payment() {}

    public Payment(int paymentId, int bookingId, int customerId, BigDecimal amount,
                   Timestamp paymentDate, String paymentMethod, String paymentStatus,
                   String transactionReference) {
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.customerId = customerId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.transactionReference = transactionReference;
    }

    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Timestamp getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Timestamp paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    @Override
    public String toString() {
        return "Payment #" + paymentId + " - ₹" + amount + " (" + paymentMethod + ")";
    }
}
