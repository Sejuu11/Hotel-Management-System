package com.hotelmanagement.ui;

import com.hotelmanagement.model.User;
import com.hotelmanagement.service.AuthService;
import com.hotelmanagement.util.UIUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Modern authentication frame for system users.
 */
public class LoginFrame extends JFrame {
    private final AuthService authService;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnExit;

    public LoginFrame() {
        this.authService = AuthService.getInstance();
        initComponents();
    }

    private void initComponents() {
        setTitle("Hotel Management System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 520);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(UIUtil.COLOR_BACKGROUND);

        // Header Banner
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(UIUtil.COLOR_PRIMARY);
        headerPanel.setBorder(new EmptyBorder(30, 20, 30, 20));

        JLabel lblTitle = new JLabel("Grand Palace Hotel");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitle = new JLabel("Property Management System");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSubtitle.setForeground(new Color(148, 163, 184));
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(lblTitle);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        headerPanel.add(lblSubtitle);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Form Card
        JPanel cardPanel = UIUtil.createCardPanel();
        cardPanel.setLayout(new GridBagLayout());
        cardPanel.setBorder(new EmptyBorder(30, 40, 30, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        // Username
        JLabel lblUser = new JLabel("Username");
        lblUser.setFont(UIUtil.FONT_BOLD);
        lblUser.setForeground(UIUtil.COLOR_TEXT_MAIN);
        gbc.gridy = 0;
        cardPanel.add(lblUser, gbc);

        txtUsername = UIUtil.createTextField(20);
        txtUsername.setPreferredSize(new Dimension(280, 38));
        gbc.gridy = 1;
        cardPanel.add(txtUsername, gbc);

        // Password
        JLabel lblPass = new JLabel("Password");
        lblPass.setFont(UIUtil.FONT_BOLD);
        lblPass.setForeground(UIUtil.COLOR_TEXT_MAIN);
        gbc.gridy = 2;
        cardPanel.add(lblPass, gbc);

        txtPassword = UIUtil.createPasswordField(20);
        txtPassword.setPreferredSize(new Dimension(280, 38));
        gbc.gridy = 3;
        cardPanel.add(txtPassword, gbc);

        // Hint info
        JLabel lblHint = new JLabel("<html><font color='#64748B' size='2'>Default: <b>admin</b> / admin123 &nbsp;|&nbsp; <b>receptionist</b> / recep123</font></html>");
        lblHint.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 4;
        gbc.insets = new Insets(12, 0, 16, 0);
        cardPanel.add(lblHint, gbc);

        // Button Panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        buttonPanel.setOpaque(false);

        btnLogin = UIUtil.createPrimaryButton("Sign In");
        btnLogin.setPreferredSize(new Dimension(130, 40));

        btnExit = UIUtil.createSecondaryButton("Exit");
        btnExit.setPreferredSize(new Dimension(130, 40));

        buttonPanel.add(btnLogin);
        buttonPanel.add(btnExit);

        gbc.gridy = 5;
        gbc.insets = new Insets(8, 0, 0, 0);
        cardPanel.add(buttonPanel, gbc);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.add(cardPanel);
        mainPanel.add(centerWrapper, BorderLayout.CENTER);

        setContentPane(mainPanel);

        // Events
        btnLogin.addActionListener(e -> performLogin());
        btnExit.addActionListener(e -> System.exit(0));

        KeyAdapter enterKey = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performLogin();
                }
            }
        };
        txtUsername.addKeyListener(enterKey);
        txtPassword.addKeyListener(enterKey);
    }

    private void performLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            UIUtil.showWarning(this, "Please enter both username and password.");
            return;
        }

        try {
            User user = authService.login(username, password);
            dispose();
            SwingUtilities.invokeLater(() -> {
                DashboardFrame dashboard = new DashboardFrame(user);
                dashboard.setVisible(true);
            });
        } catch (Exception ex) {
            UIUtil.showError(this, ex.getMessage());
            txtPassword.setText("");
            txtPassword.requestFocus();
        }
    }
}
