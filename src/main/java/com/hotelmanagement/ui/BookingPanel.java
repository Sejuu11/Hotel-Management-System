package com.hotelmanagement.ui;

import com.hotelmanagement.model.Booking;
import com.hotelmanagement.model.Customer;
import com.hotelmanagement.model.Room;
import com.hotelmanagement.service.AuthService;
import com.hotelmanagement.service.BookingService;
import com.hotelmanagement.service.CustomerService;
import com.hotelmanagement.service.RoomService;
import com.hotelmanagement.util.DateUtil;
import com.hotelmanagement.util.UIUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Reservation management panel with room availability picker, automated night
 * and rate calculation, and reservation creation.
 */
public class BookingPanel extends JPanel {
    private final BookingService bookingService;
    private final CustomerService customerService;
    private final RoomService roomService;

    private JTable tblBookings;
    private DefaultTableModel tableModel;
    private JTextField txtSearch;
    private JComboBox<String> cmbFilterStatus;

    // Reservation Form Inputs
    private JComboBox<Customer> cmbCustomers;
    private JComboBox<Room> cmbAvailableRooms;
    private JTextField txtCheckInDate;
    private JTextField txtCheckOutDate;
    private JSpinner spinGuests;
    private JTextField txtSpecialRequest;

    // Calculation preview labels
    private JLabel lblRatePreview;
    private JLabel lblNightsPreview;
    private JLabel lblTotalPreview;

    private JButton btnCreateReservation;
    private JButton btnCancelReservation;
    private JButton btnRefresh;

    public BookingPanel() {
        this.bookingService = new BookingService();
        this.customerService = new CustomerService();
        this.roomService = new RoomService();

        setLayout(new BorderLayout(16, 16));
        setBackground(UIUtil.COLOR_BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initComponents();
        refreshAll();
    }

    private void initComponents() {
        // 1. Top Bar & Filters
        JPanel topPanel = new JPanel(new BorderLayout(12, 12));
        topPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Reservation & Booking Management");
        lblTitle.setFont(UIUtil.FONT_HEADER);
        lblTitle.setForeground(UIUtil.COLOR_TEXT_MAIN);
        topPanel.add(lblTitle, BorderLayout.WEST);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterPanel.setOpaque(false);

        txtSearch = UIUtil.createTextField(12);
        txtSearch.setToolTipText("Search Guest, Phone, Room # or Booking ID");

        cmbFilterStatus = UIUtil.createComboBox(new String[]{"All", "RESERVED", "CHECKED_IN", "CHECKED_OUT", "CANCELLED"});

        JButton btnFilter = UIUtil.createSecondaryButton("Filter");
        btnFilter.addActionListener(e -> performSearch());

        btnRefresh = UIUtil.createSecondaryButton("Refresh");
        btnRefresh.addActionListener(e -> refreshAll());

        filterPanel.add(new JLabel("Search:"));
        filterPanel.add(txtSearch);
        filterPanel.add(new JLabel("Status:"));
        filterPanel.add(cmbFilterStatus);
        filterPanel.add(btnFilter);
        filterPanel.add(btnRefresh);
        topPanel.add(filterPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // 2. Center Table
        String[] columns = {"Booking ID", "Guest Name", "Phone", "Room #", "Room Type", "Check-In", "Check-Out", "Guests", "Status", "Special Request"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblBookings = new JTable(tableModel);
        UIUtil.styleTable(tblBookings);
        tblBookings.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tblBookings.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tblBookings.getSelectedRow();
                if (row >= 0) {
                    String status = String.valueOf(tableModel.getValueAt(row, 8));
                    btnCancelReservation.setEnabled("RESERVED".equalsIgnoreCase(status) || "CHECKED_IN".equalsIgnoreCase(status));
                } else {
                    btnCancelReservation.setEnabled(false);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tblBookings);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIUtil.COLOR_BORDER, 1));
        add(scrollPane, BorderLayout.CENTER);

        // 3. Bottom Reservation Card
        JPanel formCard = UIUtil.createCardPanel();
        formCard.setLayout(new BorderLayout(12, 12));

        JLabel lblFormTitle = new JLabel("Create New Reservation");
        lblFormTitle.setFont(UIUtil.FONT_SUBHEADER);
        formCard.add(lblFormTitle, BorderLayout.NORTH);

        JPanel formGrid = new JPanel(new GridLayout(2, 4, 14, 10));
        formGrid.setOpaque(false);

        cmbCustomers = new JComboBox<>();
        cmbCustomers.setFont(UIUtil.FONT_REGULAR);
        cmbCustomers.setBackground(Color.WHITE);

        cmbAvailableRooms = new JComboBox<>();
        cmbAvailableRooms.setFont(UIUtil.FONT_REGULAR);
        cmbAvailableRooms.setBackground(Color.WHITE);
        cmbAvailableRooms.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                calculateQuote();
            }
        });

        LocalDate today = LocalDate.now();
        txtCheckInDate = UIUtil.createTextField(10);
        txtCheckInDate.setText(today.toString());

        txtCheckOutDate = UIUtil.createTextField(10);
        txtCheckOutDate.setText(today.plusDays(1).toString());

        spinGuests = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        spinGuests.setFont(UIUtil.FONT_REGULAR);

        txtSpecialRequest = UIUtil.createTextField(16);

        // Live calculation triggers
        txtCheckInDate.addActionListener(e -> calculateQuote());
        txtCheckOutDate.addActionListener(e -> calculateQuote());

        formGrid.add(createFieldWrapper("Select Guest *", cmbCustomers));
        formGrid.add(createFieldWrapper("Select Available Room *", cmbAvailableRooms));
        formGrid.add(createFieldWrapper("Check-In Date (YYYY-MM-DD) *", txtCheckInDate));
        formGrid.add(createFieldWrapper("Check-Out Date (YYYY-MM-DD) *", txtCheckOutDate));
        formGrid.add(createFieldWrapper("Number of Guests *", spinGuests));
        formGrid.add(createFieldWrapper("Special Request / Remarks", txtSpecialRequest));

        // Price Quote Box
        JPanel quoteBox = new JPanel(new GridLayout(3, 1, 4, 2));
        quoteBox.setBackground(new Color(241, 245, 249));
        quoteBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIUtil.COLOR_BORDER, 1),
            new EmptyBorder(6, 12, 6, 12)
        ));

        lblRatePreview = new JLabel("Room Rate: ₹0 / night");
        lblRatePreview.setFont(UIUtil.FONT_SMALL);

        lblNightsPreview = new JLabel("Duration: 1 Night(s)");
        lblNightsPreview.setFont(UIUtil.FONT_SMALL);

        lblTotalPreview = new JLabel("Estimated Total: ₹0.00");
        lblTotalPreview.setFont(UIUtil.FONT_BOLD);
        lblTotalPreview.setForeground(UIUtil.COLOR_ACCENT);

        quoteBox.add(lblRatePreview);
        quoteBox.add(lblNightsPreview);
        quoteBox.add(lblTotalPreview);

        formGrid.add(createFieldWrapper("Cost Estimation", quoteBox));

        formCard.add(formGrid, BorderLayout.CENTER);

        // Toolbar
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        btnBar.setOpaque(false);

        JButton btnRecalc = UIUtil.createSecondaryButton("Calculate Estimate");
        btnRecalc.addActionListener(e -> calculateQuote());

        btnCreateReservation = UIUtil.createPrimaryButton("Confirm Reservation");
        btnCreateReservation.addActionListener(e -> createReservation());

        btnCancelReservation = UIUtil.createDangerButton("Cancel Selected Booking");
        btnCancelReservation.setEnabled(false);
        btnCancelReservation.addActionListener(e -> cancelSelectedBooking());

        btnBar.add(btnRecalc);
        btnBar.add(btnCreateReservation);
        btnBar.add(btnCancelReservation);

        formCard.add(btnBar, BorderLayout.SOUTH);
        add(formCard, BorderLayout.SOUTH);
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

    public void refreshAll() {
        loadCustomers();
        loadAvailableRooms();
        loadBookings();
        calculateQuote();
    }

    private void loadCustomers() {
        cmbCustomers.removeAllItems();
        List<Customer> list = customerService.getAllCustomers();
        for (Customer c : list) {
            cmbCustomers.addItem(c);
        }
    }

    private void loadAvailableRooms() {
        cmbAvailableRooms.removeAllItems();
        List<Room> list = roomService.getAvailableRooms();
        for (Room r : list) {
            cmbAvailableRooms.addItem(r);
        }
    }

    public void loadBookings() {
        tableModel.setRowCount(0);
        List<Booking> list = bookingService.getAllBookings();
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
                b.getBookingStatus(),
                b.getSpecialRequest() != null ? b.getSpecialRequest() : ""
            });
        }
    }

    private void performSearch() {
        String kw = txtSearch.getText().trim();
        String status = (String) cmbFilterStatus.getSelectedItem();
        tableModel.setRowCount(0);
        List<Booking> list = bookingService.searchBookings(kw, status);
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
                b.getBookingStatus(),
                b.getSpecialRequest() != null ? b.getSpecialRequest() : ""
            });
        }
    }

    private void calculateQuote() {
        Room r = (Room) cmbAvailableRooms.getSelectedItem();
        if (r == null) {
            lblRatePreview.setText("Room Rate: ₹0 / night");
            lblNightsPreview.setText("Duration: 0 Night(s)");
            lblTotalPreview.setText("Estimated Total: ₹0.00");
            return;
        }

        BigDecimal rate = r.getPricePerNight();
        LocalDate in = DateUtil.parseDate(txtCheckInDate.getText().trim());
        LocalDate out = DateUtil.parseDate(txtCheckOutDate.getText().trim());

        long nights = (in != null && out != null) ? DateUtil.calculateNights(in, out) : 1;
        BigDecimal est = rate.multiply(BigDecimal.valueOf(nights)).setScale(2, RoundingMode.HALF_UP);

        lblRatePreview.setText("Room Rate: ₹" + rate + " / night");
        lblNightsPreview.setText("Duration: " + nights + " Night(s)");
        lblTotalPreview.setText("Estimated Total: ₹" + est);
    }

    private void createReservation() {
        Customer cust = (Customer) cmbCustomers.getSelectedItem();
        Room room = (Room) cmbAvailableRooms.getSelectedItem();

        if (cust == null) {
            UIUtil.showWarning(this, "Please select a registered guest.");
            return;
        }
        if (room == null) {
            UIUtil.showWarning(this, "No available room selected.");
            return;
        }

        try {
            LocalDate inDate = DateUtil.parseDate(txtCheckInDate.getText().trim());
            LocalDate outDate = DateUtil.parseDate(txtCheckOutDate.getText().trim());
            int guests = (int) spinGuests.getValue();

            Booking b = new Booking();
            b.setCustomerId(cust.getCustomerId());
            b.setRoomId(room.getRoomId());
            b.setCheckInDate(inDate);
            b.setExpectedCheckOutDate(outDate);
            b.setNumberOfGuests(guests);
            b.setSpecialRequest(txtSpecialRequest.getText().trim());
            if (AuthService.getInstance().getCurrentUser() != null) {
                b.setCreatedBy(AuthService.getInstance().getCurrentUser().getId());
            }

            if (bookingService.createBooking(b)) {
                UIUtil.showSuccess(this, "Reservation #" + b.getBookingId() + " created successfully for Room " + room.getRoomNumber() + "!");
                txtSpecialRequest.setText("");
                refreshAll();
            }
        } catch (Exception ex) {
            UIUtil.showError(this, ex.getMessage());
        }
    }

    private void cancelSelectedBooking() {
        int row = tblBookings.getSelectedRow();
        if (row < 0) return;

        int bookingId = (int) tableModel.getValueAt(row, 0);
        String guestName = String.valueOf(tableModel.getValueAt(row, 1));

        if (!UIUtil.confirmAction(this, "Confirm Cancellation", "Are you sure you want to cancel booking #" + bookingId + " for " + guestName + "?")) {
            return;
        }

        try {
            if (bookingService.cancelBooking(bookingId)) {
                UIUtil.showSuccess(this, "Booking #" + bookingId + " has been cancelled and the room released.");
                refreshAll();
            }
        } catch (Exception ex) {
            UIUtil.showError(this, ex.getMessage());
        }
    }
}
