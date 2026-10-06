package com.hotelmanagement.ui;

import com.hotelmanagement.model.Booking;
import com.hotelmanagement.model.Payment;
import com.hotelmanagement.service.BookingService;
import com.hotelmanagement.service.PaymentService;
import com.hotelmanagement.util.DateUtil;
import com.hotelmanagement.util.UIUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Payment management panel for tracking payments, advance deposits, and transactions.
 */
public class PaymentPanel extends JPanel {
    private final PaymentService paymentService;
    private final BookingService bookingService;

    private JTable tblPayments;
    private DefaultTableModel tableModel;
    private JTextField txtSearch;
    private JComboBox<String> cmbFilterMethod;
    private JComboBox<String> cmbFilterStatus;

    // Metrics labels
    private JLabel lblTotalTransactions;
    private JLabel lblTotalCollected;

    // Manual payment form inputs
    private JComboBox<Booking> cmbActiveBookings;
    private JTextField txtAmount;
    private JComboBox<String> cmbMethod;
    private JComboBox<String> cmbStatus;
    private JTextField txtTxnRef;

    public PaymentPanel() {
        this.paymentService = new PaymentService();
        this.bookingService = new BookingService();

        setLayout(new BorderLayout(16, 16));
        setBackground(UIUtil.COLOR_BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initComponents();
        loadPayments();
        loadActiveBookings();
    }

    private void initComponents() {
        // 1. Top Bar & Metrics Cards
        JPanel northWrapper = new JPanel(new BorderLayout(12, 12));
        northWrapper.setOpaque(false);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JLabel lblTitle = new JLabel("Payment Ledger & Financial Transactions");
        lblTitle.setFont(UIUtil.FONT_HEADER);
        lblTitle.setForeground(UIUtil.COLOR_TEXT_MAIN);
        topBar.add(lblTitle, BorderLayout.WEST);

        // Search & Filter row
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterRow.setOpaque(false);

        txtSearch = UIUtil.createTextField(10);
        txtSearch.setToolTipText("Search Guest, Room, Ref, or Booking #");

        cmbFilterMethod = UIUtil.createComboBox(new String[]{"All", "CASH", "CARD", "UPI", "BANK_TRANSFER"});
        cmbFilterStatus = UIUtil.createComboBox(new String[]{"All", "PAID", "PARTIAL", "REFUNDED", "PENDING"});

        JButton btnFilter = UIUtil.createSecondaryButton("Filter");
        btnFilter.addActionListener(e -> performSearch());

        JButton btnReset = UIUtil.createSecondaryButton("Reset");
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            cmbFilterMethod.setSelectedIndex(0);
            cmbFilterStatus.setSelectedIndex(0);
            loadPayments();
        });

        filterRow.add(new JLabel("Search:"));
        filterRow.add(txtSearch);
        filterRow.add(new JLabel("Method:"));
        filterRow.add(cmbFilterMethod);
        filterRow.add(new JLabel("Status:"));
        filterRow.add(cmbFilterStatus);
        filterRow.add(btnFilter);
        filterRow.add(btnReset);
        topBar.add(filterRow, BorderLayout.EAST);

        northWrapper.add(topBar, BorderLayout.NORTH);

        // Summary metric cards
        JPanel summaryPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        summaryPanel.setOpaque(false);
        summaryPanel.setPreferredSize(new Dimension(800, 75));

        lblTotalTransactions = new JLabel("0");
        lblTotalCollected = new JLabel("₹0.00");

        summaryPanel.add(createMiniCard("Total Transactions Logged", lblTotalTransactions, new Color(59, 130, 246)));
        summaryPanel.add(createMiniCard("Total Collections Sum", lblTotalCollected, UIUtil.COLOR_SUCCESS));

        northWrapper.add(summaryPanel, BorderLayout.SOUTH);
        add(northWrapper, BorderLayout.NORTH);

        // 2. Center Table
        String[] columns = {"ID", "Booking #", "Guest Name", "Room #", "Amount (₹)", "Payment Date", "Method", "Status", "Reference #"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblPayments = new JTable(tableModel);
        UIUtil.styleTable(tblPayments);
        tblPayments.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(tblPayments);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIUtil.COLOR_BORDER, 1));
        add(scrollPane, BorderLayout.CENTER);

        // 3. Bottom Form Card for Recording Advance / Add-on Payment
        JPanel formCard = UIUtil.createCardPanel();
        formCard.setLayout(new BorderLayout(10, 10));

        JLabel lblFormTitle = new JLabel("Record Advance / Add-on Payment");
        lblFormTitle.setFont(UIUtil.FONT_SUBHEADER);
        formCard.add(lblFormTitle, BorderLayout.NORTH);

        JPanel gridFields = new JPanel(new GridLayout(1, 5, 12, 0));
        gridFields.setOpaque(false);

        cmbActiveBookings = new JComboBox<>();
        cmbActiveBookings.setFont(UIUtil.FONT_REGULAR);
        cmbActiveBookings.setBackground(Color.WHITE);

        txtAmount = UIUtil.createTextField(10);
        cmbMethod = UIUtil.createComboBox(new String[]{"CASH", "CARD", "UPI", "BANK_TRANSFER"});
        cmbStatus = UIUtil.createComboBox(new String[]{"PAID", "PARTIAL"});
        txtTxnRef = UIUtil.createTextField(12);

        gridFields.add(createFieldWrapper("Target Booking *", cmbActiveBookings));
        gridFields.add(createFieldWrapper("Payment Amount (₹) *", txtAmount));
        gridFields.add(createFieldWrapper("Payment Method *", cmbMethod));
        gridFields.add(createFieldWrapper("Status *", cmbStatus));
        gridFields.add(createFieldWrapper("Transaction Ref", txtTxnRef));

        formCard.add(gridFields, BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        btnBar.setOpaque(false);

        JButton btnRecord = UIUtil.createPrimaryButton("Record Payment");
        btnRecord.addActionListener(e -> recordPayment());

        JButton btnClear = UIUtil.createSecondaryButton("Clear");
        btnClear.addActionListener(e -> {
            txtAmount.setText("");
            txtTxnRef.setText("");
        });

        btnBar.add(btnRecord);
        btnBar.add(btnClear);
        formCard.add(btnBar, BorderLayout.SOUTH);

        add(formCard, BorderLayout.SOUTH);
    }

    private JPanel createMiniCard(String title, JLabel valueLabel, Color barColor) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 4, 0, 0, barColor),
            BorderFactory.createCompoundBorder(
                new LineBorder(UIUtil.COLOR_BORDER, 1),
                new EmptyBorder(8, 12, 8, 12)
            )
        ));
        JLabel lbl = new JLabel(title);
        lbl.setFont(UIUtil.FONT_SMALL);
        lbl.setForeground(UIUtil.COLOR_TEXT_MUTED);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valueLabel.setForeground(UIUtil.COLOR_TEXT_MAIN);

        card.add(lbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createFieldWrapper(String labelText, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(UIUtil.FONT_SMALL);
        lbl.setForeground(UIUtil.COLOR_TEXT_MUTED);
        p.add(lbl, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    public void loadPayments() {
        tableModel.setRowCount(0);
        List<Payment> list = paymentService.getAllPayments();
        BigDecimal totalSum = BigDecimal.ZERO;

        for (Payment p : list) {
            tableModel.addRow(new Object[]{
                p.getPaymentId(),
                "#" + p.getBookingId(),
                p.getCustomerName(),
                p.getRoomNumber(),
                p.getAmount(),
                DateUtil.formatDateTime(p.getPaymentDate()),
                p.getPaymentMethod(),
                p.getPaymentStatus(),
                p.getTransactionReference() != null ? p.getTransactionReference() : ""
            });
            if ("PAID".equalsIgnoreCase(p.getPaymentStatus()) || "PARTIAL".equalsIgnoreCase(p.getPaymentStatus())) {
                totalSum = totalSum.add(p.getAmount());
            }
        }

        lblTotalTransactions.setText(String.valueOf(list.size()));
        lblTotalCollected.setText("₹" + totalSum);
    }

    public void loadActiveBookings() {
        cmbActiveBookings.removeAllItems();
        List<Booking> list = bookingService.getAllBookings();
        for (Booking b : list) {
            if ("RESERVED".equalsIgnoreCase(b.getBookingStatus()) || "CHECKED_IN".equalsIgnoreCase(b.getBookingStatus())) {
                cmbActiveBookings.addItem(b);
            }
        }
    }

    private void performSearch() {
        String kw = txtSearch.getText().trim();
        String method = (String) cmbFilterMethod.getSelectedItem();
        String status = (String) cmbFilterStatus.getSelectedItem();

        tableModel.setRowCount(0);
        List<Payment> list = paymentService.searchPayments(kw, method, status);
        BigDecimal totalSum = BigDecimal.ZERO;

        for (Payment p : list) {
            tableModel.addRow(new Object[]{
                p.getPaymentId(),
                "#" + p.getBookingId(),
                p.getCustomerName(),
                p.getRoomNumber(),
                p.getAmount(),
                DateUtil.formatDateTime(p.getPaymentDate()),
                p.getPaymentMethod(),
                p.getPaymentStatus(),
                p.getTransactionReference() != null ? p.getTransactionReference() : ""
            });
            if ("PAID".equalsIgnoreCase(p.getPaymentStatus()) || "PARTIAL".equalsIgnoreCase(p.getPaymentStatus())) {
                totalSum = totalSum.add(p.getAmount());
            }
        }
        lblTotalTransactions.setText(String.valueOf(list.size()));
        lblTotalCollected.setText("₹" + totalSum);
    }

    private void recordPayment() {
        Booking b = (Booking) cmbActiveBookings.getSelectedItem();
        if (b == null) {
            UIUtil.showWarning(this, "Please select an active booking.");
            return;
        }

        String amtStr = txtAmount.getText().trim();
        if (amtStr.isEmpty()) {
            UIUtil.showWarning(this, "Please enter payment amount.");
            return;
        }

        BigDecimal amt;
        try {
            amt = new BigDecimal(amtStr);
        } catch (Exception e) {
            UIUtil.showWarning(this, "Invalid payment amount number.");
            return;
        }

        String method = (String) cmbMethod.getSelectedItem();
        String status = (String) cmbStatus.getSelectedItem();
        String ref = txtTxnRef.getText().trim();

        try {
            Payment p = new Payment();
            p.setBookingId(b.getBookingId());
            p.setCustomerId(b.getCustomerId());
            p.setAmount(amt);
            p.setPaymentMethod(method);
            p.setPaymentStatus(status);
            p.setTransactionReference(!ref.isEmpty() ? ref : "MANUAL-REC-" + System.currentTimeMillis());

            if (paymentService.recordPayment(p)) {
                UIUtil.showSuccess(this, "Payment of ₹" + amt + " recorded successfully for Booking #" + b.getBookingId());
                txtAmount.setText("");
                txtTxnRef.setText("");
                loadPayments();
                loadActiveBookings();
            }
        } catch (Exception ex) {
            UIUtil.showError(this, ex.getMessage());
        }
    }
}
