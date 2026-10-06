package com.hotelmanagement.ui;

import com.hotelmanagement.model.DashboardStats;
import com.hotelmanagement.service.ReportService;
import com.hotelmanagement.util.UIUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Modern dashboard panel presenting real-time operational metrics and quick actions.
 */
public class DashboardPanel extends JPanel {
    private final ReportService reportService;
    private final Consumer<String> navigationConsumer;

    private JLabel lblTotalRooms;
    private JLabel lblAvailableRooms;
    private JLabel lblOccupiedRooms;
    private JLabel lblReservedRooms;
    private JLabel lblTotalCustomers;
    private JLabel lblActiveBookings;
    private JLabel lblTodayCheckIns;
    private JLabel lblTodayRevenue;

    private DefaultTableModel modelRoomStatus;
    private DefaultTableModel modelRoomType;

    public DashboardPanel(Consumer<String> navigationConsumer) {
        this.reportService = new ReportService();
        this.navigationConsumer = navigationConsumer;
        setLayout(new BorderLayout(16, 16));
        setBackground(UIUtil.COLOR_BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initComponents();
        loadData();
    }

    private void initComponents() {
        // Top action bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JLabel lblTitle = new JLabel("Operations Dashboard");
        lblTitle.setFont(UIUtil.FONT_HEADER);
        lblTitle.setForeground(UIUtil.COLOR_TEXT_MAIN);

        JButton btnRefresh = UIUtil.createSecondaryButton("Refresh Data");
        btnRefresh.addActionListener(e -> loadData());

        topBar.add(lblTitle, BorderLayout.WEST);
        topBar.add(btnRefresh, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // Center content wrapped in JScrollPane
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        // 1. Metric Cards Grid (2 rows x 4 cols)
        JPanel statsGrid = new JPanel(new GridLayout(2, 4, 16, 16));
        statsGrid.setOpaque(false);
        statsGrid.setMaximumSize(new Dimension(3000, 210));

        lblTotalRooms = new JLabel("0");
        lblAvailableRooms = new JLabel("0");
        lblOccupiedRooms = new JLabel("0");
        lblReservedRooms = new JLabel("0");
        lblTotalCustomers = new JLabel("0");
        lblActiveBookings = new JLabel("0");
        lblTodayCheckIns = new JLabel("0");
        lblTodayRevenue = new JLabel("₹0.00");

        statsGrid.add(createStatCard("Total Rooms", lblTotalRooms, new Color(59, 130, 246)));
        statsGrid.add(createStatCard("Available Rooms", lblAvailableRooms, UIUtil.COLOR_SUCCESS));
        statsGrid.add(createStatCard("Occupied Rooms", lblOccupiedRooms, UIUtil.COLOR_WARNING));
        statsGrid.add(createStatCard("Reserved Rooms", lblReservedRooms, new Color(139, 92, 246)));
        statsGrid.add(createStatCard("Total Customers", lblTotalCustomers, new Color(14, 165, 233)));
        statsGrid.add(createStatCard("Active Bookings", lblActiveBookings, new Color(236, 72, 153)));
        statsGrid.add(createStatCard("Today's Check-ins", lblTodayCheckIns, new Color(20, 184, 166)));
        statsGrid.add(createStatCard("Today's Revenue", lblTodayRevenue, new Color(16, 185, 129)));

        centerPanel.add(statsGrid);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // 2. Quick Action Buttons
        JPanel actionCard = UIUtil.createCardPanel();
        actionCard.setLayout(new BorderLayout());
        actionCard.setMaximumSize(new Dimension(3000, 90));

        JLabel lblQuick = new JLabel("Quick Actions");
        lblQuick.setFont(UIUtil.FONT_SUBHEADER);
        lblQuick.setBorder(new EmptyBorder(0, 0, 10, 0));
        actionCard.add(lblQuick, BorderLayout.NORTH);

        JPanel actionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        actionsRow.setOpaque(false);

        JButton btnNewBooking = UIUtil.createPrimaryButton("+ New Booking");
        btnNewBooking.addActionListener(e -> navigateTo("Bookings"));

        JButton btnNewCust = UIUtil.createSecondaryButton("+ Add Guest");
        btnNewCust.addActionListener(e -> navigateTo("Customers"));

        JButton btnQuickCheckIn = UIUtil.createSecondaryButton("Check-In Guest");
        btnQuickCheckIn.addActionListener(e -> navigateTo("Check-In"));

        JButton btnQuickCheckOut = UIUtil.createSecondaryButton("Settle Check-Out");
        btnQuickCheckOut.addActionListener(e -> navigateTo("Check-Out"));

        JButton btnViewReports = UIUtil.createSecondaryButton("View Reports");
        btnViewReports.addActionListener(e -> navigateTo("Reports"));

        actionsRow.add(btnNewBooking);
        actionsRow.add(btnNewCust);
        actionsRow.add(btnQuickCheckIn);
        actionsRow.add(btnQuickCheckOut);
        actionsRow.add(btnViewReports);

        actionCard.add(actionsRow, BorderLayout.CENTER);
        centerPanel.add(actionCard);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // 3. Tables Row (Room Status breakdown & Room Type breakdown)
        JPanel tablesRow = new JPanel(new GridLayout(1, 2, 16, 16));
        tablesRow.setOpaque(false);
        tablesRow.setPreferredSize(new Dimension(1000, 260));

        // Status Card
        JPanel cardStatus = UIUtil.createCardPanel();
        cardStatus.setLayout(new BorderLayout(8, 8));
        JLabel lblStatusHeader = new JLabel("Room Status Breakdown");
        lblStatusHeader.setFont(UIUtil.FONT_SUBHEADER);
        cardStatus.add(lblStatusHeader, BorderLayout.NORTH);

        modelRoomStatus = new DefaultTableModel(new String[]{"Status Category", "Number of Rooms"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tblStatus = new JTable(modelRoomStatus);
        UIUtil.styleTable(tblStatus);
        cardStatus.add(new JScrollPane(tblStatus), BorderLayout.CENTER);

        // Type Card
        JPanel cardType = UIUtil.createCardPanel();
        cardType.setLayout(new BorderLayout(8, 8));
        JLabel lblTypeHeader = new JLabel("Room Category Inventory");
        lblTypeHeader.setFont(UIUtil.FONT_SUBHEADER);
        cardType.add(lblTypeHeader, BorderLayout.NORTH);

        modelRoomType = new DefaultTableModel(new String[]{"Room Type", "Total Units"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tblType = new JTable(modelRoomType);
        UIUtil.styleTable(tblType);
        cardType.add(new JScrollPane(tblType), BorderLayout.CENTER);

        tablesRow.add(cardStatus);
        tablesRow.add(cardType);
        centerPanel.add(tablesRow);

        JScrollPane scrollPane = new JScrollPane(centerPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 4, 0, 0, accentColor),
            BorderFactory.createCompoundBorder(
                new LineBorder(UIUtil.COLOR_BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)
            )
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(UIUtil.FONT_SMALL);
        lblTitle.setForeground(UIUtil.COLOR_TEXT_MUTED);

        valueLabel.setFont(UIUtil.FONT_CARD_VALUE);
        valueLabel.setForeground(UIUtil.COLOR_TEXT_MAIN);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    public void loadData() {
        SwingWorker<DashboardStats, Void> worker = new SwingWorker<>() {
            @Override
            protected DashboardStats doInBackground() {
                return reportService.getDashboardStatistics();
            }

            @Override
            protected void done() {
                try {
                    DashboardStats stats = get();
                    lblTotalRooms.setText(String.valueOf(stats.getTotalRooms()));
                    lblAvailableRooms.setText(String.valueOf(stats.getAvailableRooms()));
                    lblOccupiedRooms.setText(String.valueOf(stats.getOccupiedRooms()));
                    lblReservedRooms.setText(String.valueOf(stats.getReservedRooms()));
                    lblTotalCustomers.setText(String.valueOf(stats.getTotalCustomers()));
                    lblActiveBookings.setText(String.valueOf(stats.getActiveBookings()));
                    lblTodayCheckIns.setText(String.valueOf(stats.getTodayCheckIns()));
                    lblTodayRevenue.setText("₹" + stats.getTodayRevenue());

                    // Populate status breakdown
                    modelRoomStatus.setRowCount(0);
                    for (Map.Entry<String, Integer> e : stats.getRoomsByStatus().entrySet()) {
                        modelRoomStatus.addRow(new Object[]{e.getKey(), e.getValue()});
                    }

                    // Populate type breakdown
                    modelRoomType.setRowCount(0);
                    for (Map.Entry<String, Integer> e : stats.getRoomsByType().entrySet()) {
                        modelRoomType.addRow(new Object[]{e.getKey(), e.getValue()});
                    }
                } catch (Exception ex) {
                    UIUtil.showError(DashboardPanel.this, "Failed to load dashboard metrics: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void navigateTo(String destination) {
        if (navigationConsumer != null) {
            navigationConsumer.accept(destination);
        }
    }
}
