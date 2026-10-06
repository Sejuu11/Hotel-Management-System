package com.hotelmanagement.util;

import com.hotelmanagement.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationUtilTest {

    @ParameterizedTest
    @ValueSource(strings = {"user@example.com", "test.name+tag@domain.co.in", "admin@hotel.org"})
    @DisplayName("Should accept valid emails")
    public void testValidEmails(String email) {
        assertDoesNotThrow(() -> ValidationUtil.validateEmail(email));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-email", "user@", "@domain.com", "user@domain"})
    @DisplayName("Should reject invalid emails")
    public void testInvalidEmails(String email) {
        assertThrows(ValidationException.class, () -> ValidationUtil.validateEmail(email));
    }

    @ParameterizedTest
    @ValueSource(strings = {"9876543210", "+91 9876543210", "011-2345678", "(022) 23456789"})
    @DisplayName("Should accept valid phone numbers")
    public void testValidPhones(String phone) {
        assertDoesNotThrow(() -> ValidationUtil.validatePhone(phone));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "123456", "phone", ""})
    @DisplayName("Should reject invalid phone numbers")
    public void testInvalidPhones(String phone) {
        assertThrows(ValidationException.class, () -> ValidationUtil.validatePhone(phone));
    }

    @Test
    @DisplayName("Should validate date range correctly")
    public void testDateRange() {
        LocalDate in = LocalDate.of(2026, 10, 10);
        LocalDate out = LocalDate.of(2026, 10, 12);
        assertDoesNotThrow(() -> ValidationUtil.validateBookingDates(in, out));

        // Same day or reverse
        assertThrows(ValidationException.class, () -> ValidationUtil.validateBookingDates(in, in));
        assertThrows(ValidationException.class, () -> ValidationUtil.validateBookingDates(out, in));
    }

    @Test
    @DisplayName("Should validate positive monetary amount")
    public void testPositiveAmount() {
        assertDoesNotThrow(() -> ValidationUtil.validatePositiveNumber(new BigDecimal("100.50"), "Amount"));
        assertThrows(ValidationException.class, () -> ValidationUtil.validatePositiveNumber(BigDecimal.ZERO, "Amount"));
        assertThrows(ValidationException.class, () -> ValidationUtil.validatePositiveNumber(new BigDecimal("-50.00"), "Amount"));
    }
}
