package vn.hcmute.jpaweb.utils;

import at.favre.lib.crypto.bcrypt.BCrypt;

public final class PasswordUtil {

    public static final int BCRYPT_COST = 12;

    private PasswordUtil() {
    }

    public static String hash(String rawValue) {
        if (rawValue == null || rawValue.isEmpty()) {
            throw new IllegalArgumentException("Value to hash must not be empty");
        }
        return BCrypt.withDefaults().hashToString(BCRYPT_COST, rawValue.toCharArray());
    }

    public static boolean matches(String rawValue, String encodedValue) {
        if (rawValue == null || rawValue.isEmpty()
                || encodedValue == null || encodedValue.isBlank()) {
            return false;
        }

        try {
            return BCrypt.verifyer().verify(rawValue.toCharArray(), encodedValue).verified;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
