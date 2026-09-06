package vn.hcmute.jpaweb.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OTPUtilTest {

    @Test
    void generateCodeAlwaysReturnsSixDigits() {
        for (int index = 0; index < 100; index++) {
            assertTrue(OTPUtil.generateCode().matches("\\d{6}"));
        }
    }

    @Test
    void hashStoresNoPlaintextAndMatchesOnlyTheCorrectCode() {
        String code = "012345";

        String codeHash = OTPUtil.hash(code);

        assertFalse(code.equals(codeHash));
        assertTrue(OTPUtil.matches(code, codeHash));
        assertTrue(OTPUtil.matches(" 012345 ", codeHash));
        assertFalse(OTPUtil.matches("543210", codeHash));
        assertFalse(OTPUtil.matches("12345", codeHash));
    }

    @Test
    void hashRejectsCodesThatAreNotExactlySixDigits() {
        assertThrows(IllegalArgumentException.class, () -> OTPUtil.hash(null));
        assertThrows(IllegalArgumentException.class, () -> OTPUtil.hash("12345"));
        assertThrows(IllegalArgumentException.class, () -> OTPUtil.hash("1234567"));
        assertThrows(IllegalArgumentException.class, () -> OTPUtil.hash("12A456"));
    }

    @Test
    void expiryAndResetAuthorizationUseConfiguredDurations() {
        LocalDateTime issuedAt = LocalDateTime.of(2026, 9, 6, 12, 0);

        assertEquals(issuedAt.plusMinutes(5), OTPUtil.expiryFrom(issuedAt));
        assertEquals(issuedAt.plusMinutes(10), OTPUtil.resetAuthorizationExpiryFrom(issuedAt));
    }

    @Test
    void expiryFromNowIsFiveMinutesFromInvocation() {
        LocalDateTime earliest = LocalDateTime.now().plus(OTPUtil.VALIDITY);
        LocalDateTime expiry = OTPUtil.expiryFromNow();
        LocalDateTime latest = LocalDateTime.now().plus(OTPUtil.VALIDITY);

        assertFalse(expiry.isBefore(earliest));
        assertFalse(expiry.isAfter(latest));
    }

    @Test
    void resendCooldownAllowsTheSixtiethSecond() {
        LocalDateTime sentAt = LocalDateTime.of(2026, 9, 6, 12, 0);

        assertFalse(OTPUtil.canResend(sentAt, sentAt.plusSeconds(59)));
        assertTrue(OTPUtil.canResend(sentAt, sentAt.plusSeconds(60)));
        assertTrue(OTPUtil.canResend(null, sentAt));
    }
}
