package com.hotelmanagement.service;

import com.hotelmanagement.model.BillDetails;
import com.hotelmanagement.util.DateUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class BusinessLogicTest {

    @Test
    @DisplayName("Should correctly calculate number of nights with minimum 1 night")
    public void testNightCalculation() {
        LocalDate d1 = LocalDate.of(2026, 10, 1);
        LocalDate d2 = LocalDate.of(2026, 10, 5);
        assertEquals(4, DateUtil.calculateNights(d1, d2));

        // Same day stay should default to at least 1 night
        assertEquals(1, DateUtil.calculateNights(d1, d1));
    }

    @Test
    @DisplayName("Should accurately calculate invoice bill breakdown and balance due")
    public void testBillCalculation() {
        BillDetails bill = new BillDetails();
        bill.setPricePerNight(new BigDecimal("2000.00"));
        bill.setNumberOfNights(3); // Room charge = 6000.00
        bill.setAdditionalCharges(new BigDecimal("500.00")); // Subtotal = 6500.00
        bill.setTaxRate(new BigDecimal("0.12")); // 12% GST = 780.00 -> Grand Total = 7280.00
        bill.setPaidAmount(new BigDecimal("2000.00")); // Advance paid = 2000.00

        bill.recalculate();

        assertEquals(new BigDecimal("6000.00"), bill.getRoomCharges());
        assertEquals(new BigDecimal("780.00"), bill.getTaxAmount());
        assertEquals(new BigDecimal("7280.00"), bill.getGrandTotal());
        assertEquals(new BigDecimal("5280.00"), bill.getBalanceDue());
    }

    @Test
    @DisplayName("Should handle fully paid bills without negative balance")
    public void testFullyPaidBill() {
        BillDetails bill = new BillDetails();
        bill.setPricePerNight(new BigDecimal("1500.00"));
        bill.setNumberOfNights(2); // 3000.00
        bill.setTaxRate(new BigDecimal("0.12")); // 360.00 -> Grand Total = 3360.00
        bill.setPaidAmount(new BigDecimal("4000.00")); // Overpaid / rounded up

        bill.recalculate();

        assertEquals(new BigDecimal("3360.00"), bill.getGrandTotal());
        assertEquals(BigDecimal.ZERO.setScale(2), bill.getBalanceDue());
    }
}
