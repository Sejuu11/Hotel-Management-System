package com.hotelmanagement;

import com.formdev.flatlaf.FlatLightLaf;
import com.hotelmanagement.database.DatabaseConnection;
import com.hotelmanagement.ui.LoginFrame;
import com.hotelmanagement.util.UIUtil;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main application launcher for the Hotel Management System.
 */
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // Set modern FlatLaf look and feel
        try {
            FlatLightLaf.setup();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to initialize FlatLaf, falling back to System Look and Feel", e);
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to set System Look and Feel", ex);
            }
        }

        // Test database connectivity
        boolean dbOk = DatabaseConnection.testConnection();
        if (!dbOk) {
            LOGGER.warning("Initial database connection test failed.");
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                    null,
                    "Warning: Could not connect to MySQL database 'hotel_management'.\n" +
                    "Please ensure:\n" +
                    "1. MySQL Server 8.0 is running.\n" +
                    "2. You have executed database/hotel_management.sql.\n" +
                    "3. Credentials in db.properties or environment variables are correct.\n\n" +
                    "The application will launch, but database operations may fail until configured.",
                    "Database Warning",
                    JOptionPane.WARNING_MESSAGE
                );
            });
        }

        // Launch Login Frame on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
