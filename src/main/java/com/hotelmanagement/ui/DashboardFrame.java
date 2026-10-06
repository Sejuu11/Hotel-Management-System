package com.hotelmanagement.ui;

import com.hotelmanagement.model.User;
import com.hotelmanagement.service.AuthService;
import com.hotelmanagement.util.DateUtil;
import com.hotelmanagement.util.UIUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Main application window featuring responsive sidebar navigation, role-based
 * module access, and active tab transitions.
 */
public class DashboardFrame extends JFrame {
    private final User currentUser;

    private CardLayout cardLayout;
    private JPanel contentPanel;
    private Map<String, JButton> navButtons = new HashMap<>();

    // Sub-panels
    private DashboardPanel dashboardPanel;
    private CustomerPanel customerPanel;
    private EmployeePanel employeePanel;
    private RoomPanel roomPanel;
    private BookingPanel bookingPanel;
    private CheckInPanel checkInPanel;
    private CheckOutPanel checkOutPanel;
    private PaymentPanel paymentPanel;
    private ReportsPanel reportsPanel;

    public DashboardFrame(User user) {
        this.currentUser = user;
        initComponents();
        showView("Dashboard");
    }

    private void initComponents() {
        setTitle("Grand Palace Hotel Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1320, 850);
        setMinimumSize(new Dimension(1024, 700));
        setLocationRelativeTo(null);

        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(UIUtil.COLOR_BACKGROUND);

        // 1. Top Header Bar
        JPanel topHeader = createHeaderBar();
        rootPanel.add(topHeader, BorderLayout.NORTH);

        // 2. Left Sidebar Navigation
        JPanel sidebar = createSidebar();
        rootPanel.add(sidebar, BorderLayout.WEST);

        // 3. Center Content Area (CardLayout)
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(UIUtil.COLOR_BACKGROUND);

        // Initialize panels
        dashboardPanel = new DashboardPanel(this::showView);
        customerPanel = new CustomerPanel();
        roomPanel = new RoomPanel();
        bookingPanel = new BookingPanel();
        checkInPanel = new CheckInPanel();
        checkOutPanel = new CheckOutPanel();
        paymentPanel = new PaymentPanel();
        reportsPanel = new ReportsPanel();

        contentPanel.add(dashboardPanel, "Dashboard");
        contentPanel.add(customerPanel, "Customers");
        contentPanel.add(roomPanel, "Rooms");
        contentPanel.add(bookingPanel, "Bookings");
        contentPanel.add(checkInPanel, "Check-In");
        contentPanel.add(checkOutPanel, "Check-Out");
        contentPanel.add(paymentPanel, "Payments");
        contentPanel.add(reportsPanel, "Reports");

        // Role-based access: Only ADMIN can access Employees module
        if (currentUser != null && currentUser.isAdmin()) {
            employeePanel = new EmployeePanel();
            contentPanel.add(employeePanel, "Employees");
        }

        rootPanel.add(contentPanel, BorderLayout.CENTER);
        setContentPane(rootPanel);
    }

    private JPanel createHeaderBar() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIUtil.COLOR_PRIMARY);
        header.setPreferredSize(new Dimension(getWidth(), 64));
        header.setBorder(new EmptyBorder(12, 24, 12, 24));

        // Brand Title
        JPanel leftBrand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftBrand.setOpaque(false);

        JLabel lblLogo = new JLabel("✦ GRAND PALACE HOTEL");
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblLogo.setForeground(Color.WHITE);

        JLabel lblTagline = new JLabel("| Property Management Suite");
        lblTagline.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblTagline.setForeground(new Color(148, 163, 184));

        leftBrand.add(lblLogo);
        leftBrand.add(lblTagline);
        header.add(leftBrand, BorderLayout.WEST);

        // User badge and logout
        JPanel rightInfo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        rightInfo.setOpaque(false);

        JLabel lblDate = new JLabel(DateUtil.formatDate(LocalDate.now()));
        lblDate.setFont(UIUtil.FONT_REGULAR);
        lblDate.setForeground(new Color(203, 213, 225));

        String userDisplay = currentUser != null ? currentUser.getFullName() : "Administrator";
        JLabel lblUser = new JLabel(userDisplay);
        lblUser.setFont(UIUtil.FONT_BOLD);
        lblUser.setForeground(Color.WHITE);

        String role = currentUser != null ? currentUser.getRole() : "ADMIN";
        JLabel lblRoleBadge = new JLabel(" " + role + " ");
        lblRoleBadge.setFont(UIUtil.FONT_SMALL);
        lblRoleBadge.setOpaque(true);
        if ("ADMIN".equalsIgnoreCase(role)) {
            lblRoleBadge.setBackground(new Color(239, 68, 68)); // Red for Admin
            lblRoleBadge.setForeground(Color.WHITE);
        } else {
            lblRoleBadge.setBackground(new Color(59, 130, 246)); // Blue for Receptionist
            lblRoleBadge.setForeground(Color.WHITE);
        }
        lblRoleBadge.setBorder(new EmptyBorder(2, 6, 2, 6));

        JButton btnLogout = UIUtil.createSecondaryButton("Sign Out");
        btnLogout.setPreferredSize(new Dimension(90, 32));
        btnLogout.addActionListener(e -> performLogout());

        rightInfo.add(lblDate);
        rightInfo.add(new JSeparator(SwingConstants.VERTICAL));
        rightInfo.add(lblUser);
        rightInfo.add(lblRoleBadge);
        rightInfo.add(btnLogout);

        header.add(rightInfo, BorderLayout.EAST);
        return header;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UIUtil.COLOR_PRIMARY_LIGHT);
        sidebar.setPreferredSize(new Dimension(210, getHeight()));
        sidebar.setBorder(new EmptyBorder(16, 10, 16, 10));

        String[] navItems;
        // Role check: hide Employees for Receptionist
        if (currentUser != null && currentUser.isAdmin()) {
            navItems = new String[]{
                "Dashboard", "Customers", "Employees", "Rooms",
                "Bookings", "Check-In", "Check-Out", "Payments", "Reports"
            };
        } else {
            navItems = new String[]{
                "Dashboard", "Customers", "Rooms",
                "Bookings", "Check-In", "Check-Out", "Payments", "Reports"
            };
        }

        for (String item : navItems) {
            JButton btn = createNavButton(item);
            navButtons.put(item, btn);
            sidebar.add(btn);
            sidebar.add(Box.createRigidArea(new Dimension(0, 6)));
        }

        sidebar.add(Box.createVerticalGlue());

        // Logout item at bottom
        JButton btnBottomLogout = createNavButton("Logout");
        btnBottomLogout.addActionListener(e -> performLogout());
        sidebar.add(btnBottomLogout);

        return sidebar;
    }

    private JButton createNavButton(String name) {
        JButton btn = new JButton(name);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setForeground(new Color(226, 232, 240));
        btn.setBackground(UIUtil.COLOR_PRIMARY_LIGHT);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(190, 42));
        btn.setPreferredSize(new Dimension(190, 42));
        btn.setBorder(new EmptyBorder(10, 16, 10, 16));

        btn.addActionListener(e -> {
            if ("Logout".equalsIgnoreCase(name)) {
                performLogout();
            } else {
                showView(name);
            }
        });

        return btn;
    }

    public void showView(String viewName) {
        // Highlight active nav button
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            JButton b = entry.getValue();
            if (entry.getKey().equalsIgnoreCase(viewName)) {
                b.setBackground(UIUtil.COLOR_ACCENT);
                b.setForeground(Color.WHITE);
                b.setFont(new Font("Segoe UI", Font.BOLD, 14));
            } else {
                b.setBackground(UIUtil.COLOR_PRIMARY_LIGHT);
                b.setForeground(new Color(226, 232, 240));
                b.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            }
        }

        cardLayout.show(contentPanel, viewName);

        // Trigger dynamic refresh on panel activation
        switch (viewName) {
            case "Dashboard" -> dashboardPanel.loadData();
            case "Customers" -> customerPanel.loadCustomers();
            case "Employees" -> {
                if (employeePanel != null) employeePanel.loadEmployees();
            }
            case "Rooms" -> roomPanel.loadRooms();
            case "Bookings" -> bookingPanel.refreshAll();
            case "Check-In" -> checkInPanel.loadReservedBookings();
            case "Check-Out" -> checkOutPanel.loadActiveBookings();
            case "Payments" -> {
                paymentPanel.loadPayments();
                paymentPanel.loadActiveBookings();
            }
            case "Reports" -> reportsPanel.loadAllReports();
        }
    }

    private void performLogout() {
        if (UIUtil.confirmAction(this, "Sign Out", "Are you sure you want to end your current session?")) {
            AuthService.getInstance().logout();
            dispose();
            SwingUtilities.invokeLater(() -> {
                LoginFrame login = new LoginFrame();
                login.setVisible(true);
            });
        }
    }
}
