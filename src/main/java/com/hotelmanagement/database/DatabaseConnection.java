package com.hotelmanagement.database;

import com.hotelmanagement.config.DatabaseConfig;
import com.hotelmanagement.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages database connectivity, transactions, and connection lifecycle.
 */
public class DatabaseConnection {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "MySQL JDBC Driver not found in classpath", e);
            throw new DatabaseException("MySQL JDBC Driver could not be loaded. Please ensure mysql-connector-j is included.", e);
        }
    }

    /**
     * Obtains a new JDBC connection using configured credentials.
     * Caller is responsible for closing the connection (e.g. try-with-resources).
     */
    public static Connection getConnection() throws DatabaseException {
        try {
            return DriverManager.getConnection(
                DatabaseConfig.getUrl(),
                DatabaseConfig.getUsername(),
                DatabaseConfig.getPassword()
            );
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to establish database connection to: " + DatabaseConfig.getUrl(), e);
            throw new DatabaseException("Unable to connect to the database. Please verify MySQL service is running and credentials are valid.", e);
        }
    }

    /**
     * Verifies whether the database is accessible.
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Database connection test failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Safely rolls back a transaction on the provided connection.
     */
    public static void rollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to rollback transaction", e);
            }
        }
    }

    /**
     * Safely resets auto-commit and closes the connection.
     */
    public static void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.getAutoCommit()) {
                    conn.setAutoCommit(true);
                }
                conn.close();
            } catch (SQLException e) {
                LOGGER.log(Level.FINE, "Error closing connection", e);
            }
        }
    }
}
