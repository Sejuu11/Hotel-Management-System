package com.hotelmanagement.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles database configuration loading with environment variable overrides
 * and properties file support. Never hardcodes production credentials.
 */
public class DatabaseConfig {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConfig.class.getName());
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/hotel_management?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static String url;
    private static String username;
    private static String password;

    static {
        loadConfiguration();
    }

    private static void loadConfiguration() {
        Properties props = new Properties();

        // 1. Try to load from classpath (src/main/resources/db.properties)
        try (InputStream in = DatabaseConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load db.properties from classpath", e);
        }

        // 2. If not loaded or empty, try from current working directory
        if (props.isEmpty()) {
            File localFile = new File("db.properties");
            if (localFile.exists()) {
                try (FileInputStream fis = new FileInputStream(localFile)) {
                    props.load(fis);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Could not load local db.properties file", e);
                }
            }
        }

        // 3. Environment variables take highest precedence
        String envUrl = System.getenv("DB_URL");
        String envUser = System.getenv("DB_USER");
        if (envUser == null) {
            envUser = System.getenv("DB_USERNAME");
        }
        String envPass = System.getenv("DB_PASSWORD");

        url = envUrl != null ? envUrl : props.getProperty("DB_URL", props.getProperty("db.url", DEFAULT_URL));
        username = envUser != null ? envUser : props.getProperty("DB_USER", props.getProperty("db.user", "root"));
        password = envPass != null ? envPass : props.getProperty("DB_PASSWORD", props.getProperty("db.password", ""));
    }

    public static String getUrl() {
        return url;
    }

    public static String getUsername() {
        return username;
    }

    public static String getPassword() {
        return password;
    }

    public static void setCredentials(String newUrl, String newUser, String newPass) {
        url = newUrl;
        username = newUser;
        password = newPass;
    }
}
