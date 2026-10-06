package com.hotelmanagement.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Secure password hashing utility using SHA-256 with cryptographic salting.
 */
public class PasswordUtil {
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Hashes a plaintext password with a freshly generated 16-byte random salt.
     * Output format: "saltHex:hashHex"
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        String saltHex = bytesToHex(salt);
        String hashHex = computeHash(saltHex, plainPassword);
        return saltHex + ":" + hashHex;
    }

    /**
     * Verifies a candidate password against a stored hash string.
     * Supports both "salt:hash" format and legacy direct SHA-256 hashes.
     */
    public static boolean verifyPassword(String candidatePassword, String storedHash) {
        if (candidatePassword == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }

        if (storedHash.contains(":")) {
            String[] parts = storedHash.split(":", 2);
            if (parts.length != 2) {
                return false;
            }
            String saltHex = parts[0];
            String expectedHashHex = parts[1];
            String actualHashHex = computeHash(saltHex, candidatePassword);
            return slowEquals(expectedHashHex, actualHashHex);
        } else {
            // Legacy / direct SHA-256 hash comparison
            String candidateHashHex = computeRawSha256(candidatePassword);
            return slowEquals(storedHash, candidateHashHex);
        }
    }

    private static String computeHash(String saltHex, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(saltHex.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static String computeRawSha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     */
    private static boolean slowEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        int diff = a.length() ^ b.length();
        for (int i = 0; i < a.length() && i < b.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
