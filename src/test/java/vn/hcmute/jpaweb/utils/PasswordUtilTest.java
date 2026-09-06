package vn.hcmute.jpaweb.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {

    @Test
    void hashUsesBCryptCostTwelveAndMatchesOriginalValue() {
        String rawValue = "StrongPass123!";

        String encodedValue = PasswordUtil.hash(rawValue);

        assertNotEquals(rawValue, encodedValue);
        assertTrue(encodedValue.matches("^\\$2[aby]\\$12\\$.+"));
        assertTrue(PasswordUtil.matches(rawValue, encodedValue));
        assertFalse(PasswordUtil.matches("WrongPass123!", encodedValue));
    }

    @Test
    void hashUsesAUniqueSalt() {
        String first = PasswordUtil.hash("SamePassword123!");
        String second = PasswordUtil.hash("SamePassword123!");

        assertNotEquals(first, second);
        assertTrue(PasswordUtil.matches("SamePassword123!", first));
        assertTrue(PasswordUtil.matches("SamePassword123!", second));
    }

    @Test
    void hashRejectsMissingValues() {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash(""));
    }

    @Test
    void matchesRejectsMissingOrMalformedValues() {
        assertFalse(PasswordUtil.matches(null, "$2a$12$invalid"));
        assertFalse(PasswordUtil.matches("password", null));
        assertFalse(PasswordUtil.matches("password", "not-a-bcrypt-hash"));
    }
}
