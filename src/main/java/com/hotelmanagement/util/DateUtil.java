package com.hotelmanagement.util;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Utility for parsing, formatting, and calculating date ranges.
 */
public class DateUtil {
    public static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    public static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DISPLAY_DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    public static Date toSqlDate(LocalDate localDate) {
        return localDate == null ? null : Date.valueOf(localDate);
    }

    public static LocalDate toLocalDate(Date sqlDate) {
        return sqlDate == null ? null : sqlDate.toLocalDate();
    }

    public static Timestamp toTimestamp(LocalDateTime localDateTime) {
        return localDateTime == null ? null : Timestamp.valueOf(localDateTime);
    }

    public static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    public static String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DISPLAY_DATE_FORMAT);
    }

    public static String formatDate(Date date) {
        return date == null ? "" : date.toLocalDate().format(DISPLAY_DATE_FORMAT);
    }

    public static String formatDateTime(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().format(DISPLAY_DATETIME_FORMAT);
    }

    public static LocalDate parseDate(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        text = text.trim();
        try {
            return LocalDate.parse(text, ISO_DATE_FORMAT);
        } catch (Exception e) {
            try {
                return LocalDate.parse(text, DISPLAY_DATE_FORMAT);
            } catch (Exception ex) {
                return LocalDate.parse(text);
            }
        }
    }

    /**
     * Calculates nights between check-in and check-out. Minimum 1 night.
     */
    public static long calculateNights(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            return 1;
        }
        long days = ChronoUnit.DAYS.between(checkIn, checkOut);
        return days <= 0 ? 1 : days;
    }
}
