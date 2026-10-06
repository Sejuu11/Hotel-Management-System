package com.hotelmanagement.util;

import com.hotelmanagement.exception.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Validates user inputs across all management modules.
 */
public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[0-9+\\-\\s()]{7,20}$"
    );

    public static void requireNonEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " is required and cannot be empty.");
        }
    }

    public static void validateEmail(String email) {
        if (email != null && !email.trim().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
                throw new ValidationException("Invalid email format: " + email);
            }
        }
    }

    public static void validatePhone(String phone) {
        requireNonEmpty(phone, "Phone number");
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < 7 || digits.length() > 15) {
            throw new ValidationException("Phone number must contain between 7 and 15 digits.");
        }
    }

    public static void validatePositiveNumber(BigDecimal amount, String fieldName) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(fieldName + " must be greater than zero.");
        }
    }

    public static void validatePositiveInteger(int value, String fieldName) {
        if (value <= 0) {
            throw new ValidationException(fieldName + " must be greater than zero.");
        }
    }

    public static void validateDateRange(LocalDate startDate, LocalDate endDate, String startName, String endName) {
        if (startDate == null) {
            throw new ValidationException(startName + " is required.");
        }
        if (endDate == null) {
            throw new ValidationException(endName + " is required.");
        }
        if (endDate.isBefore(startDate)) {
            throw new ValidationException(endName + " cannot be before " + startName + ".");
        }
    }

    public static void validateBookingDates(LocalDate checkIn, LocalDate checkOut) {
        validateDateRange(checkIn, checkOut, "Check-in date", "Check-out date");
        if (checkIn.isEqual(checkOut)) {
            throw new ValidationException("Check-out date must be at least one day after check-in date.");
        }
    }
}
