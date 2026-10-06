package com.hotelmanagement.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordUtilTest {

    @Test
    @DisplayName("Should generate valid salted hash and verify correctly")
    public void testHashAndVerify() {
        String plain = "MySecretPassword123!";
        String hash = PasswordUtil.hashPassword(plain);

        assertNotNull(hash);
        assertTrue(hash.contains(":"), "Hash format should contain salt separator ':'");

        assertTrue(PasswordUtil.verifyPassword(plain, hash), "Password should match its own hash");
        assertFalse(PasswordUtil.verifyPassword("WrongPassword", hash), "Wrong password should not match");
    }

    @Test
    @DisplayName("Should verify seed admin and receptionist hashes")
    public void testSeedHashes() {
        String adminHash = "a1b2c3d4e5f67890:6fcf2c472dbf4cf3fb36657e173743e5a2345489da639f38263f5f4a2234ad1a";
        String recepHash = "b2c3d4e5f6a17890:2920d0341c92dbe01b57b295fe0e56654a93037f0742a7827e5d5112d9fa8166";

        assertTrue(PasswordUtil.verifyPassword("admin123", adminHash));
        assertFalse(PasswordUtil.verifyPassword("wrongpass", adminHash));

        assertTrue(PasswordUtil.verifyPassword("recep123", recepHash));
        assertFalse(PasswordUtil.verifyPassword("wrongpass", recepHash));
    }

    @Test
    @DisplayName("Should reject empty or null inputs safely")
    public void testNullOrEmptyInputs() {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword(""));

        assertFalse(PasswordUtil.verifyPassword(null, "hash"));
        assertFalse(PasswordUtil.verifyPassword("pass", null));
        assertFalse(PasswordUtil.verifyPassword("pass", ""));
    }
}
