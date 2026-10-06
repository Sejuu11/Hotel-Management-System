package com.hotelmanagement.ui;

import com.hotelmanagement.model.BillDetails;
import com.hotelmanagement.model.Booking;
import com.hotelmanagement.service.BookingService;
import com.hotelmanagement.service.CheckInOutService;
import com.hotelmanagement.util.UIUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Check-Out and billing settlement panel with live invoice generation and atomic checkout.
 */
public class CheckOutPanel extends JPanel {
    private final BookingService bookingService;
    private final CheckInOutService checkInOutService;

    private JTable tblActiveBookings;
    private DefaultTableModel tableModel;
    private JButton btnRefresh;

    private int selectedBookingId = 0;
    private BillDetails currentBill;

    // Bill invoice display labels
    private JLabel lblGuestName;
    private JLabel lblRoomInfo;
    private JLabel lblStayPeriod;
    private JLabel lblNights;
    private JLabel lblRoomCharges;
    private JTextField txtAdditionalCharges;
    private JLabel lblTaxAmount;
    private JLabel lblGrandTotal;
    private JLabel lblPaidAmount;
    private JLabel lblBalanceDue;

    // Settlement inputs
    private JTextField txtPaymentAmount;
    private JComboBox<String> cmbPaymentMethod;
    private JTextField txtTxnRef;
    private JButton btnCompleteCheckOut;

    public CheckOutPanel() {
        this.bookingService = new BookingService();
        this.checkInOutService = new CheckInOutService();

        setLayout(new BorderLayout(16, 16));
        setBackground(UIUtil.COLOR_BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initComponents();
        loadActiveBookings();
    }

    private void initComponents() {
        // 1. Top Bar
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Front Desk Check-Out & Final Billing Settlement");
        lblTitle.setFont(UIUtil.FONT_HEADER);
        lblTitle.setForeground(UIUtil.COLOR_TEXT_MAIN);

        btnRefresh = UIUtil.createSecondaryButton("Refresh Active Stays");
        btnRefresh.addActionListener(e -> loadActiveBookings());

        topPanel.add(lblTitle, BorderLayout.WEST);
        topPanel.add(btnRefresh, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 2. Split view: Left = Table of occupied bookings, Right = Interactive Invoice & Settlement
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.52);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);

        // Left Table
        String[] columns = {"ID", "Guest", "Phone", "Room #", "Type", "Check-In Date", "Expected Check-Out"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblActiveBookings = new JTable(tableModel);
        UIUtil.styleTable(tblActiveBookings);
        tblActiveBookings.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tblActiveBookings.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onBookingSelected();
            }
        });

        JScrollPane scrollTable = new JScrollPane(tblActiveBookings);
        scrollTable.setBorder(BorderFactory.createLineBorder(UIUtil.COLOR_BORDER, 1));
        splitPane.setLeftComponent(scrollTable);

        // Right Invoice Card
        JPanel invoiceCard = UIUtil.createCardPanel();
        invoiceCard.setLayout(new BorderLayout(12, 12));

        JLabel lblInvoiceHeader = new JLabel("Guest Invoice & Settlement");
        lblInvoiceHeader.setFont(UIUtil.FONT_SUBHEADER);
        invoiceCard.add(lblInvoiceHeader, BorderLayout.NORTH);

        JPanel invoiceBody = new JPanel();
        invoiceBody.setLayout(new BoxLayout(invoiceBody, BoxLayout.Y_AXIS));
        invoiceBody.setOpaque(false);

        lblGuestName = new JLabel("Select an occupied room from the list");
        lblGuestName.setFont(UIUtil.FONT_BOLD);

        lblRoomInfo = new JLabel("-");
        lblStayPeriod = new JLabel("-");
        lblNights = new JLabel("-");
        lblRoomCharges = new JLabel("₹0.00");

        txtAdditionalCharges = UIUtil.createTextField(8);
        txtAdditionalCharges.setText("0.00");
        txtAdditionalCharges.addActionListener(e -> recalculateBill());

        lblTaxAmount = new JLabel("₹0.00 (12% GST)");
        lblGrandTotal = new JLabel("₹0.00");
        lblGrandTotal.setFont(UIUtil.FONT_BOLD);
        lblGrandTotal.setForeground(UIUtil.COLOR_PRIMARY);

        lblPaidAmount = new JLabel("₹0.00");
        lblPaidAmount.setForeground(UIUtil.COLOR_SUCCESS);

        lblBalanceDue = new JLabel("₹0.00");
        lblBalanceDue.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblBalanceDue.setForeground(UIUtil.COLOR_DANGER);

        JPanel gridBill = new JPanel(new GridLayout(9, 2, 8, 6));
        gridBill.setOpaque(false);

        gridBill.add(new JLabel("Guest:"));
        gridBill.add(lblGuestName);

        gridBill.add(new JLabel("Room:"));
        gridBill.add(lblRoomInfo);

        gridBill.add(new JLabel("Stay Dates:"));
        gridBill.add(lblStayPeriod);

        gridBill.add(new JLabel("Billable Nights:"));
        gridBill.add(lblNights);

        gridBill.add(new JLabel("Room Charges:"));
        gridBill.add(lblRoomCharges);

        JPanel pExtra = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pExtra.setOpaque(false);
        pExtra.add(txtAdditionalCharges);
        JButton btnApply = UIUtil.createSecondaryButton("Apply");
        btnApply.addActionListener(e -> recalculateBill());
        pExtra.add(btnApply);

        gridBill.add(new JLabel("Extra Charges (₹):"));
        gridBill.add(pExtra);

        gridBill.add(new JLabel("Taxes (12% GST):"));
        gridBill.add(lblTaxAmount);

        gridBill.add(new JLabel("Grand Total:"));
        gridBill.add(lblGrandTotal);

        gridBill.add(new JLabel("Advance Paid:"));
        gridBill.add(lblPaidAmount);

        invoiceBody.add(gridBill);
        invoiceBody.add(Box.createRigidArea(new Dimension(0, 10)));

        // Balance Due highlight banner
        JPanel bannerBalance = new JPanel(new BorderLayout());
        bannerBalance.setBackground(new Color(254, 242, 242));
        bannerBalance.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(254, 202, 202), 1),
            new EmptyBorder(8, 12, 8, 12)
        ));
        JLabel lblBalTitle = new JLabel("Balance Due To Pay:");
        lblBalTitle.setFont(UIUtil.FONT_BOLD);
        lblBalTitle.setForeground(new Color(153, 27, 27));
        bannerBalance.add(lblBalTitle, BorderLayout.WEST);
        bannerBalance.add(lblBalanceDue, BorderLayout.EAST);
        invoiceBody.add(bannerBalance);
        invoiceBody.add(Box.createRigidArea(new Dimension(0, 14)));

        // Payment Settlement inputs
        JPanel settlementGrid = new JPanel(new GridLayout(3, 2, 8, 8));
        settlementGrid.setOpaque(false);

        txtPaymentAmount = UIUtil.createTextField(10);
        cmbPaymentMethod = UIUtil.createComboBox(new String[]{"CASH", "CARD", "UPI", "BANK_TRANSFER"});
        txtTxnRef = UIUtil.createTextField(14);
        txtTxnRef.setToolTipText("Transaction ID / Cash receipt voucher #");

        settlementGrid.add(new JLabel("Settle Amount (₹):"));
        settlementGrid.add(txtPaymentAmount);

        settlementGrid.add(new JLabel("Payment Method:"));
        settlementGrid.add(cmbPaymentMethod);

        settlementGrid.add(new JLabel("Transaction Ref:"));
        settlementGrid.add(txtTxnRef);

        invoiceBody.add(settlementGrid);
        invoiceCard.add(invoiceBody, BorderLayout.CENTER);

        // Settlement Action Button
        btnCompleteCheckOut = UIUtil.createSuccessButton("Complete Check-Out & Generate Receipt");
        btnCompleteCheckOut.setPreferredSize(new Dimension(300, 42));
        btnCompleteCheckOut.setEnabled(false);
        btnCompleteCheckOut.addActionListener(e -> executeCheckOut());

        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        pBtn.setOpaque(false);
        pBtn.add(btnCompleteCheckOut);
        invoiceCard.add(pBtn, BorderLayout.SOUTH);

        splitPane.setRightComponent(invoiceCard);
        add(splitPane, BorderLayout.CENTER);
    }

    public void loadActiveBookings() {
        tableModel.setRowCount(0);
        List<Booking> list = bookingService.getBookingsByStatus("CHECKED_IN");
        for (Booking b : list) {
            tableModel.addRow(new Object[]{
                b.getBookingId(),
                b.getCustomerName(),
                b.getCustomerPhone(),
                b.getRoomNumber(),
                b.getRoomType(),
                b.getCheckInDate(),
                b.getExpectedCheckOutDate()
            });
        }
        resetInvoice();
    }

    private void onBookingSelected() {
        int row = tblActiveBookings.getSelectedRow();
        if (row >= 0) {
            selectedBookingId = (int) tableModel.getValueAt(row, 0);
            recalculateBill();
            btnCompleteCheckOut.setEnabled(true);
        } else {
            resetInvoice();
        }
    }

    private void recalculateBill() {
        if (selectedBookingId <= 0) return;

        BigDecimal extra = BigDecimal.ZERO;
        try {
            String extraStr = txtAdditionalCharges.getText().trim();
            if (!extraStr.isEmpty()) {
                extra = new BigDecimal(extraStr);
            }
        } catch (Exception ignored) {}

        try {
            currentBill = checkInOutService.calculateBill(selectedBookingId, extra, new BigDecimal("0.12"));

            lblGuestName.setText(currentBill.getCustomerName() + " (" + currentBill.getCustomerPhone() + ")");
            lblRoomInfo.setText("Room " + currentBill.getRoomNumber() + " (" + currentBill.getRoomType() + " @ ₹" + currentBill.getPricePerNight() + "/night)");
            lblStayPeriod.setText(currentBill.getCheckInDate() + " to " + currentBill.getCheckOutDate());
            lblNights.setText(currentBill.getNumberOfNights() + " Night(s)");
            lblRoomCharges.setText("₹" + currentBill.getRoomCharges());
            lblTaxAmount.setText("₹" + currentBill.getTaxAmount() + " (12% GST)");
            lblGrandTotal.setText("₹" + currentBill.getGrandTotal());
            lblPaidAmount.setText("₹" + currentBill.getPaidAmount());
            lblBalanceDue.setText("₹" + currentBill.getBalanceDue());

            // Auto-fill settlement payment with remaining balance
            txtPaymentAmount.setText(currentBill.getBalanceDue().toString());
        } catch (Exception ex) {
            UIUtil.showError(this, ex.getMessage());
        }
    }

    private void resetInvoice() {
        selectedBookingId = 0;
        currentBill = null;
        lblGuestName.setText("Select an occupied room from the list");
        lblRoomInfo.setText("-");
        lblStayPeriod.setText("-");
        lblNights.setText("-");
        lblRoomCharges.setText("₹0.00");
        txtAdditionalCharges.setText("0.00");
        lblTaxAmount.setText("₹0.00");
        lblGrandTotal.setText("₹0.00");
        lblPaidAmount.setText("₹0.00");
        lblBalanceDue.setText("₹0.00");
        txtPaymentAmount.setText("");
        txtTxnRef.setText("");
        btnCompleteCheckOut.setEnabled(false);
    }

    private void executeCheckOut() {
        if (selectedBookingId <= 0 || currentBill == null) return;

        BigDecimal payAmt = BigDecimal.ZERO;
        String payStr = txtPaymentAmount.getText().trim();
        if (!payStr.isEmpty()) {
            try {
                payAmt = new BigDecimal(payStr);
            } catch (Exception e) {
                UIUtil.showWarning(this, "Please enter a valid numeric payment amount.");
                return;
            }
        }

        String method = (String) cmbPaymentMethod.getSelectedItem();
        String ref = txtTxnRef.getText().trim();

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Complete checkout for " + currentBill.getCustomerName() + " (Room " + currentBill.getRoomNumber() + ")?\n" +
            "Total Bill: ₹" + currentBill.getGrandTotal() + "\n" +
            "Payment Recorded: ₹" + payAmt + " (" + method + ")",
            "Confirm Checkout & Settlement",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            BigDecimal extra = currentBill.getAdditionalCharges();
            boolean success = checkInOutService.checkOut(selectedBookingId, extra, currentBill.getTaxRate(), payAmt, method, ref);
            if (success) {
                showReceiptDialog(currentBill, payAmt, method, ref);
                loadActiveBookings();
            }
        } catch (Exception ex) {
            UIUtil.showError(this, ex.getMessage());
        }
    }

    private void showReceiptDialog(BillDetails b, BigDecimal paidNow, String method, String ref) {
        StringBuilder sb = new StringBuilder();
        sb.append("==================================================\n");
        sb.append("           GRAND PALACE HOTEL & RESORT            \n");
        sb.append("             OFFICIAL CHECKOUT RECEIPT            \n");
        sb.append("==================================================\n\n");
        sb.append("Booking ID:       #").append(b.getBookingId()).append("\n");
        sb.append("Guest Name:       ").append(b.getCustomerName()).append("\n");
        sb.append("Phone:            ").append(b.getCustomerPhone()).append("\n");
        sb.append("Room:             ").append(b.getRoomNumber()).append(" (").append(b.getRoomType()).append(")\n");
        sb.append("Check-In Date:    ").append(b.getCheckInDate()).append("\n");
        sb.append("Check-Out Date:   ").append(b.getCheckOutDate()).append("\n");
        sb.append("Stay Duration:    ").append(b.getNumberOfNights()).append(" Night(s)\n");
        sb.append("--------------------------------------------------\n");
        sb.append(String.format("Room Charges:     ₹ %,10.2f\n", b.getRoomCharges()));
        sb.append(String.format("Extra Charges:    ₹ %,10.2f\n", b.getAdditionalCharges()));
        sb.append(String.format("GST / Taxes (12%%):₹ %,10.2f\n", b.getTaxAmount()));
        sb.append("--------------------------------------------------\n");
        sb.append(String.format("Grand Total:      ₹ %,10.2f\n", b.getGrandTotal()));
        sb.append(String.format("Previously Paid:  ₹ %,10.2f\n", b.getPaidAmount()));
        sb.append(String.format("Settled At Checkout: ₹ %,10.2f (%s)\n", paidNow, method));
        if (ref != null && !ref.isEmpty()) {
            sb.append("Ref / Voucher:    ").append(ref).append("\n");
        }
        sb.append("==================================================\n");
        sb.append("       Thank you for staying with us! Safe travels! \n");

        JTextArea txt = new JTextArea(sb.toString());
        txt.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txt.setEditable(false);
        txt.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane sp = new JScrollPane(txt);
        sp.setPreferredSize(new Dimension(480, 420));

        JOptionPane.showMessageDialog(this, sp, "Settlement Receipt - Booking #" + b.getBookingId(), JOptionPane.INFORMATION_MESSAGE);
    }
}
