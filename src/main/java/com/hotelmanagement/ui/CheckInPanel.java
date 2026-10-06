package com.hotelmanagement.ui;

import com.hotelmanagement.model.Booking;
import com.hotelmanagement.service.BookingService;
import com.hotelmanagement.service.CheckInOutService;
import com.hotelmanagement.util.UIUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Check-In management panel to verify guest reservations and check guests into their rooms.
 */
public class CheckInPanel extends JPanel {
    private final BookingService bookingService;
    private final CheckInOutService checkInOutService;

    private JTable tblReserved;
    private DefaultTableModel tableModel;
    private JButton btnCheckIn;
    private JButton btnRefresh;

    // Selected booking summary card
    private JLabel lblSelectedGuest;
    private JLabel lblSelectedPhone;
    private JLabel lblSelectedRoom;
    private JLabel lblSelectedDates;
    private JLabel lblSelectedGuests;

    private int selectedBookingId = 0;

    public CheckInPanel() {
        this.bookingService = new BookingService();
        this.checkInOutService = new CheckInOutService();

        setLayout(new BorderLayout(16, 16));
        setBackground(UIUtil.COLOR_BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initComponents();
        loadReservedBookings();
    }

    private void initComponents() {
        // 1. Top Bar
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Front Desk Check-In");
        lblTitle.setFont(UIUtil.FONT_HEADER);
        lblTitle.setForeground(UIUtil.COLOR_TEXT_MAIN);

        btnRefresh = UIUtil.createSecondaryButton("Refresh Reservations");
        btnRefresh.addActionListener(e -> loadReservedBookings());

        topPanel.add(lblTitle, BorderLayout.WEST);
        topPanel.add(btnRefresh, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 2. Table of RESERVED bookings
        String[] columns = {"Booking ID", "Guest Name", "Phone", "Room #", "Room Type", "Check-In Date", "Expected Check-Out", "Guests", "Special Request"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblReserved = new JTable(tableModel);
        UIUtil.styleTable(tblReserved);
        tblReserved.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tblReserved.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                populateSelection();
            }
        });

        JScrollPane scrollPane = new JScrollPane(tblReserved);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIUtil.COLOR_BORDER, 1));
        add(scrollPane, BorderLayout.CENTER);

        // 3. Bottom Verification & Action Card
        JPanel cardAction = UIUtil.createCardPanel();
        cardAction.setLayout(new BorderLayout(14, 14));

        JLabel lblCardTitle = new JLabel("Reservation Verification & Room Allocation");
        lblCardTitle.setFont(UIUtil.FONT_SUBHEADER);
        cardAction.add(lblCardTitle, BorderLayout.NORTH);

        JPanel detailsGrid = new JPanel(new GridLayout(2, 3, 16, 10));
        detailsGrid.setOpaque(false);

        lblSelectedGuest = new JLabel("None selected");
        lblSelectedGuest.setFont(UIUtil.FONT_BOLD);

        lblSelectedPhone = new JLabel("-");
        lblSelectedPhone.setFont(UIUtil.FONT_REGULAR);

        lblSelectedRoom = new JLabel("-");
        lblSelectedRoom.setFont(UIUtil.FONT_BOLD);
        lblSelectedRoom.setForeground(UIUtil.COLOR_ACCENT);

        lblSelectedDates = new JLabel("-");
        lblSelectedDates.setFont(UIUtil.FONT_REGULAR);

        lblSelectedGuests = new JLabel("-");
        lblSelectedGuests.setFont(UIUtil.FONT_REGULAR);

        detailsGrid.add(createFieldPreview("Guest Name", lblSelectedGuest));
        detailsGrid.add(createFieldPreview("Contact Phone", lblSelectedPhone));
        detailsGrid.add(createFieldPreview("Assigned Room", lblSelectedRoom));
        detailsGrid.add(createFieldPreview("Stay Period", lblSelectedDates));
        detailsGrid.add(createFieldPreview("Number of Guests", lblSelectedGuests));

        cardAction.add(detailsGrid, BorderLayout.CENTER);

        // Action button
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnBar.setOpaque(false);

        btnCheckIn = UIUtil.createSuccessButton("Confirm Check-In & Issue Room Key");
        btnCheckIn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCheckIn.setPreferredSize(new Dimension(320, 42));
        btnCheckIn.setEnabled(false);
        btnCheckIn.addActionListener(e -> executeCheckIn());

        btnBar.add(btnCheckIn);
        cardAction.add(btnBar, BorderLayout.SOUTH);

        add(cardAction, BorderLayout.SOUTH);
    }

    private JPanel createFieldPreview(String labelText, JLabel valueLabel) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(UIUtil.FONT_SMALL);
        lbl.setForeground(UIUtil.COLOR_TEXT_MUTED);
        p.add(lbl, BorderLayout.NORTH);
        p.add(valueLabel, BorderLayout.CENTER);
        return p;
    }

    public void loadReservedBookings() {
        tableModel.setRowCount(0);
        List<Booking> list = bookingService.getBookingsByStatus("RESERVED");
        for (Booking b : list) {
            tableModel.addRow(new Object[]{
                b.getBookingId(),
                b.getCustomerName(),
                b.getCustomerPhone(),
                b.getRoomNumber(),
                b.getRoomType(),
                b.getCheckInDate(),
                b.getExpectedCheckOutDate(),
                b.getNumberOfGuests(),
                b.getSpecialRequest() != null ? b.getSpecialRequest() : ""
            });
        }
        resetPreview();
    }

    private void populateSelection() {
        int row = tblReserved.getSelectedRow();
        if (row >= 0) {
            selectedBookingId = (int) tableModel.getValueAt(row, 0);
            lblSelectedGuest.setText(String.valueOf(tableModel.getValueAt(row, 1)));
            lblSelectedPhone.setText(String.valueOf(tableModel.getValueAt(row, 2)));
            lblSelectedRoom.setText("Room " + tableModel.getValueAt(row, 3) + " (" + tableModel.getValueAt(row, 4) + ")");
            lblSelectedDates.setText(tableModel.getValueAt(row, 5) + " to " + tableModel.getValueAt(row, 6));
            lblSelectedGuests.setText(String.valueOf(tableModel.getValueAt(row, 7)) + " Guest(s)");

            btnCheckIn.setEnabled(true);
        } else {
            resetPreview();
        }
    }

    private void resetPreview() {
        selectedBookingId = 0;
        lblSelectedGuest.setText("None selected");
        lblSelectedPhone.setText("-");
        lblSelectedRoom.setText("-");
        lblSelectedDates.setText("-");
        lblSelectedGuests.setText("-");
        btnCheckIn.setEnabled(false);
    }

    private void executeCheckIn() {
        if (selectedBookingId <= 0) return;

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Proceed with check-in for " + lblSelectedGuest.getText() + " into " + lblSelectedRoom.getText() + "?",
            "Confirm Check-In",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            if (checkInOutService.checkIn(selectedBookingId)) {
                UIUtil.showSuccess(this, "Guest " + lblSelectedGuest.getText() + " has successfully checked in!\nRoom is now marked as OCCUPIED.");
                loadReservedBookings();
            }
        } catch (Exception ex) {
            UIUtil.showError(this, ex.getMessage());
        }
    }
}
