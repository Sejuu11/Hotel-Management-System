package com.hotelmanagement.service;

import com.hotelmanagement.dao.ReportDAO;
import com.hotelmanagement.model.DashboardStats;

import java.time.LocalDate;
import java.util.List;

/**
 * Service providing statistical reports, multi-table joins, and aggregate metrics.
 */
public class ReportService {
    private final ReportDAO reportDAO;

    public ReportService() {
        this.reportDAO = new ReportDAO();
    }

    public ReportService(ReportDAO reportDAO) {
        this.reportDAO = reportDAO;
    }

    public DashboardStats getDashboardStatistics() {
        return reportDAO.getDashboardStatistics();
    }

    public List<Object[]> getBookingReport(LocalDate fromDate, LocalDate toDate, String status) {
        return reportDAO.getBookingReport(fromDate, toDate, status);
    }

    public List<Object[]> getRevenueReport() {
        return reportDAO.getRevenueReport();
    }

    public List<Object[]> getRoomReport() {
        return reportDAO.getRoomReport();
    }

    public List<Object[]> getEmployeeReport() {
        return reportDAO.getEmployeeReport();
    }
}
