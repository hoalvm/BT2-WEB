package vn.hcmute.jpaweb.utils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

public final class OTPUtil {

    public static final int CODE_LENGTH = 6;
    public static final int MAX_ATTEMPTS = 5;
    public static final Duration VALIDITY = Duration.ofMinutes(5);
    public static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    public static final Duration RESET_AUTHORIZATION_VALIDITY = Duration.ofMinutes(10);

    private static final int CODE_BOUND = 1_000_000;
    private static final Pattern CODE_PATTERN = Pattern.compile("\\d{" + CODE_LENGTH + "}");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private OTPUtil() {
    }

    public static String generateCode() {
        return String.format(Locale.ROOT, "%0" + CODE_LENGTH + "d", SECURE_RANDOM.nextInt(CODE_BOUND));
    }

    public static String hash(String code) {
        validateCode(code);
        return PasswordUtil.hash(code);
    }

    public static boolean matches(String code, String codeHash) {
        return code != null
                && CODE_PATTERN.matcher(code.trim()).matches()
                && PasswordUtil.matches(code.trim(), codeHash);
    }

    public static LocalDateTime expiryFromNow() {
        return LocalDateTime.now().plus(VALIDITY);
    }

    public static LocalDateTime expiryFrom(LocalDateTime sentAt) {
        if (sentAt == null) {
            throw new IllegalArgumentException("OTP sent time must not be null");
        }
        return sentAt.plus(VALIDITY);
    }

    public static LocalDateTime resetAuthorizationExpiryFrom(LocalDateTime verifiedAt) {
        if (verifiedAt == null) {
            throw new IllegalArgumentException("OTP verification time must not be null");
        }
        return verifiedAt.plus(RESET_AUTHORIZATION_VALIDITY);
    }

    public static boolean canResend(LocalDateTime lastSentAt, LocalDateTime now) {
        return lastSentAt == null
                || now == null
                || !now.isBefore(lastSentAt.plus(RESEND_COOLDOWN));
    }

    private static void validateCode(String code) {
        if (code == null || !CODE_PATTERN.matcher(code.trim()).matches()) {
            throw new IllegalArgumentException("OTP must contain exactly 6 digits");
        }
    }
}
