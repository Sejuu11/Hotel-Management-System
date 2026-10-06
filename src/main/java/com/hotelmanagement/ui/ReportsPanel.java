package com.hotelmanagement.ui;

import com.hotelmanagement.service.ReportService;
import com.hotelmanagement.util.DateUtil;
import com.hotelmanagement.util.UIUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.PrinterException;
import java.time.LocalDate;
import java.util.List;

/**
 * Analytical reports panel featuring multi-table JOINs, GROUP BY aggregations,
 * and reporting on Bookings, Revenue, Room Occupancy, and Staff Payroll.
 */
public class ReportsPanel extends JPanel {
    private final ReportService reportService;

    private JTabbedPane tabbedPane;

    // Tab 1: Bookings Report
    private JTable tblBookingReport;
    private DefaultTableModel modelBookingReport;
    private JTextField txtFromDate;
    private JTextField txtToDate;
    private JComboBox<String> cmbBookingStatus;

    // Tab 2: Revenue Report
    private JTable tblRevenueReport;
    private DefaultTableModel modelRevenueReport;

    // Tab 3: Room Report
    private JTable tblRoomReport;
    private DefaultTableModel modelRoomReport;

    // Tab 4: Employee Report
    private JTable tblEmployeeReport;
    private DefaultTableModel modelEmployeeReport;

    public ReportsPanel() {
        this.reportService = new ReportService();

        setLayout(new BorderLayout(16, 16));
        setBackground(UIUtil.COLOR_BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initComponents();
        loadAllReports();
    }

    private void initComponents() {
        // Top Header & Print Action
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("System Reports & Analytics");
        lblTitle.setFont(UIUtil.FONT_HEADER);
        lblTitle.setForeground(UIUtil.COLOR_TEXT_MAIN);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);

        JButton btnPrint = UIUtil.createSecondaryButton("Print Active Report");
        btnPrint.addActionListener(e -> printCurrentTabTable());

        JButton btnRefresh = UIUtil.createSecondaryButton("Refresh All");
        btnRefresh.addActionListener(e -> loadAllReports());

        btnRow.add(btnPrint);
        btnRow.add(btnRefresh);

        topPanel.add(lblTitle, BorderLayout.WEST);
        topPanel.add(btnRow, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Tabbed Pane for the 4 Reports
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UIUtil.FONT_BOLD);

        tabbedPane.addTab("Reservations & Stay Ledger", createBookingReportTab());
        tabbedPane.addTab("Revenue Breakdown (GROUP BY)", createRevenueReportTab());
        tabbedPane.addTab("Room Utilization & Pricing", createRoomReportTab());
        tabbedPane.addTab("Staff Payroll by Department", createEmployeeReportTab());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createBookingReportTab() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(14, 14, 14, 14));

        // Filters row
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterRow.setOpaque(false);

        txtFromDate = UIUtil.createTextField(10);
        txtToDate = UIUtil.createTextField(10);
        cmbBookingStatus = UIUtil.createComboBox(new String[]{"All", "RESERVED", "CHECKED_IN", "CHECKED_OUT", "CANCELLED"});

        JButton btnApply = UIUtil.createPrimaryButton("Filter Dates");
        btnApply.addActionListener(e -> loadBookingReport());

        JButton btnReset = UIUtil.createSecondaryButton("Clear Dates");
        btnReset.addActionListener(e -> {
            txtFromDate.setText("");
            txtToDate.setText("");
            cmbBookingStatus.setSelectedIndex(0);
            loadBookingReport();
        });

        filterRow.add(new JLabel("From Date:"));
        filterRow.add(txtFromDate);
        filterRow.add(new JLabel("To Date:"));
        filterRow.add(txtToDate);
        filterRow.add(new JLabel("Status:"));
        filterRow.add(cmbBookingStatus);
        filterRow.add(btnApply);
        filterRow.add(btnReset);

        p.add(filterRow, BorderLayout.NORTH);

        String[] cols = {"Booking #", "Customer Name", "Phone", "Room #", "Room Type", "Check-In", "Exp. Check-Out", "Act. Check-Out", "Status", "Total Settled"};
        modelBookingReport = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblBookingReport = new JTable(modelBookingReport);
        UIUtil.styleTable(tblBookingReport);

        p.add(new JScrollPane(tblBookingReport), BorderLayout.CENTER);
        return p;
    }

    private JPanel createRevenueReportTab() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel lblInfo = new JLabel("<html><b>Revenue Analysis</b> grouped by transaction date & payment method with aggregate metrics (COUNT, SUM, AVG, MIN, MAX).</html>");
        lblInfo.setFont(UIUtil.FONT_REGULAR);
        lblInfo.setForeground(UIUtil.COLOR_TEXT_MUTED);
        p.add(lblInfo, BorderLayout.NORTH);

        String[] cols = {"Transaction Date", "Payment Method", "Total Txns (COUNT)", "Total Collected (SUM)", "Avg Payment (AVG)", "Min Payment (MIN)", "Max Payment (MAX)"};
        modelRevenueReport = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblRevenueReport = new JTable(modelRevenueReport);
        UIUtil.styleTable(tblRevenueReport);

        p.add(new JScrollPane(tblRevenueReport), BorderLayout.CENTER);
        return p;
    }

    private JPanel createRoomReportTab() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel lblInfo = new JLabel("<html><b>Room Utilization & Pricing Report</b> grouped by room category with operational status breakdown.</html>");
        lblInfo.setFont(UIUtil.FONT_REGULAR);
        lblInfo.setForeground(UIUtil.COLOR_TEXT_MUTED);
        p.add(lblInfo, BorderLayout.NORTH);

        String[] cols = {"Room Type", "Total Units", "Available", "Occupied", "Reserved", "Maintenance", "Average Price", "Min Price", "Max Price"};
        modelRoomReport = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblRoomReport = new JTable(modelRoomReport);
        UIUtil.styleTable(tblRoomReport);

        p.add(new JScrollPane(tblRoomReport), BorderLayout.CENTER);
        return p;
    }

    private JPanel createEmployeeReportTab() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel lblInfo = new JLabel("<html><b>Staff & Payroll Report</b> grouped by designation with department payroll aggregations.</html>");
        lblInfo.setFont(UIUtil.FONT_REGULAR);
        lblInfo.setForeground(UIUtil.COLOR_TEXT_MUTED);
        p.add(lblInfo, BorderLayout.NORTH);

        String[] cols = {"Designation", "Staff Count", "Active Count", "Total Payroll (SUM)", "Avg Salary (AVG)", "Min Salary", "Max Salary"};
        modelEmployeeReport = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblEmployeeReport = new JTable(modelEmployeeReport);
        UIUtil.styleTable(tblEmployeeReport);

        p.add(new JScrollPane(tblEmployeeReport), BorderLayout.CENTER);
        return p;
    }

    public void loadAllReports() {
        loadBookingReport();
        loadRevenueReport();
        loadRoomReport();
        loadEmployeeReport();
    }

    private void loadBookingReport() {
        LocalDate from = DateUtil.parseDate(txtFromDate.getText().trim());
        LocalDate to = DateUtil.parseDate(txtToDate.getText().trim());
        String status = (String) cmbBookingStatus.getSelectedItem();

        modelBookingReport.setRowCount(0);
        List<Object[]> rows = reportService.getBookingReport(from, to, status);
        for (Object[] row : rows) {
            modelBookingReport.addRow(row);
        }
    }

    private void loadRevenueReport() {
        modelRevenueReport.setRowCount(0);
        List<Object[]> rows = reportService.getRevenueReport();
        for (Object[] row : rows) {
            modelRevenueReport.addRow(row);
        }
    }

    private void loadRoomReport() {
        modelRoomReport.setRowCount(0);
        List<Object[]> rows = reportService.getRoomReport();
        for (Object[] row : rows) {
            modelRoomReport.addRow(row);
        }
    }

    private void loadEmployeeReport() {
        modelEmployeeReport.setRowCount(0);
        List<Object[]> rows = reportService.getEmployeeReport();
        for (Object[] row : rows) {
            modelEmployeeReport.addRow(row);
        }
    }

    private void printCurrentTabTable() {
        JTable activeTable = null;
        String title = "";
        int tabIdx = tabbedPane.getSelectedIndex();
        switch (tabIdx) {
            case 0 -> { activeTable = tblBookingReport; title = "Hotel Management - Booking Report"; }
            case 1 -> { activeTable = tblRevenueReport; title = "Hotel Management - Revenue Summary"; }
            case 2 -> { activeTable = tblRoomReport; title = "Hotel Management - Room Utilization Report"; }
            case 3 -> { activeTable = tblEmployeeReport; title = "Hotel Management - Staff Payroll Report"; }
        }

        if (activeTable != null) {
            try {
                boolean complete = activeTable.print(JTable.PrintMode.FIT_WIDTH, new java.text.MessageFormat(title), new java.text.MessageFormat("Page {0}"));
                if (complete) {
                    UIUtil.showSuccess(this, "Report printed successfully.");
                }
            } catch (PrinterException ex) {
                UIUtil.showError(this, "Printing failed: " + ex.getMessage());
            }
        }
    }
}
