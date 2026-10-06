package com.hotelmanagement.service;

import com.hotelmanagement.dao.UserDAO;
import com.hotelmanagement.exception.ValidationException;
import com.hotelmanagement.model.User;
import com.hotelmanagement.util.PasswordUtil;
import com.hotelmanagement.util.ValidationUtil;

/**
 * Service managing user authentication, password verification, and active session state.
 */
public class AuthService {
    private static AuthService instance;
    private final UserDAO userDAO;
    private User currentUser;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    /**
     * Authenticates a user with username and password.
     */
    public User login(String username, String password) {
        ValidationUtil.requireNonEmpty(username, "Username");
        ValidationUtil.requireNonEmpty(password, "Password");

        User user = userDAO.findByUsername(username.trim());
        if (user == null) {
            throw new ValidationException("Invalid username or password.");
        }

        if (!PasswordUtil.verifyPassword(password, user.getPassword())) {
            throw new ValidationException("Invalid username or password.");
        }

        this.currentUser = user;
        return user;
    }

    public void logout() {
        this.currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }
}
