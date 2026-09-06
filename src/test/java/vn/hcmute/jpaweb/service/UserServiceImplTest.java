package vn.hcmute.jpaweb.service;

import org.junit.jupiter.api.Test;
import vn.hcmute.jpaweb.dao.IOtpTokenDao;
import vn.hcmute.jpaweb.dao.IUserDao;
import vn.hcmute.jpaweb.entity.OtpPurpose;
import vn.hcmute.jpaweb.entity.OtpToken;
import vn.hcmute.jpaweb.entity.User;
import vn.hcmute.jpaweb.utils.OTPUtil;
import vn.hcmute.jpaweb.utils.PasswordUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceImplTest {

    @Test
    void registrationOtpActivatesAccountAndLoginUsesTheHash() {
        FakeUserDao userDao = new FakeUserDao();
        FakeOtpTokenDao otpTokenDao = new FakeOtpTokenDao();
        CapturingEmailService emailService = new CapturingEmailService();
        UserServiceImpl service = new UserServiceImpl(userDao, otpTokenDao, emailService);

        User user = service.register(
                "student01",
                "student@example.com",
                "StrongPass123!",
                "StrongPass123!"
        );

        assertFalse(user.isActive());
        assertNotEquals("StrongPass123!", user.getPasswordHash());
        assertTrue(PasswordUtil.matches("StrongPass123!", user.getPasswordHash()));
        assertNotNull(emailService.lastCode);
        assertEquals(OtpPurpose.REGISTER, emailService.lastPurpose);
        assertNotEquals(emailService.lastCode, otpTokenDao.latest.getCodeHash());

        service.verifyOtp(user.getUserId(), OtpPurpose.REGISTER, emailService.lastCode);

        assertTrue(user.isActive());
        assertTrue(otpTokenDao.latest.isUsed());
        assertSame(user, service.authenticate("STUDENT@EXAMPLE.COM", "StrongPass123!"));
        assertThrows(
                IllegalStateException.class,
                () -> service.verifyOtp(user.getUserId(), OtpPurpose.REGISTER, emailService.lastCode)
        );
    }

    @Test
    void fifthWrongOtpConsumesTheToken() {
        FakeUserDao userDao = new FakeUserDao();
        FakeOtpTokenDao otpTokenDao = new FakeOtpTokenDao();
        User user = inactiveUser(userDao, "student02", "second@example.com");
        OtpToken token = activeToken(user, "123456");
        otpTokenDao.replaceActive(token);
        UserServiceImpl service = new UserServiceImpl(
                userDao,
                otpTokenDao,
                new CapturingEmailService()
        );

        for (int attempt = 0; attempt < OTPUtil.MAX_ATTEMPTS - 1; attempt++) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.verifyOtp(user.getUserId(), OtpPurpose.REGISTER, "000000")
            );
        }

        assertThrows(
                IllegalStateException.class,
                () -> service.verifyOtp(user.getUserId(), OtpPurpose.REGISTER, "000000")
        );
        assertEquals(0, token.getAttemptsRemaining());
        assertTrue(token.isUsed());
        assertFalse(user.isActive());
    }

    @Test
    void expiredOtpIsConsumedEvenWhenTheCodeIsCorrect() {
        FakeUserDao userDao = new FakeUserDao();
        FakeOtpTokenDao otpTokenDao = new FakeOtpTokenDao();
        User user = inactiveUser(userDao, "student03", "third@example.com");
        OtpToken token = activeToken(user, "654321");
        token.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        otpTokenDao.replaceActive(token);
        UserServiceImpl service = new UserServiceImpl(
                userDao,
                otpTokenDao,
                new CapturingEmailService()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.verifyOtp(user.getUserId(), OtpPurpose.REGISTER, "654321")
        );
        assertTrue(token.isUsed());
        assertFalse(user.isActive());
    }

    @Test
    void verifiedResetOtpChangesPasswordAndCanOnlyBeConsumedOnce() {
        FakeUserDao userDao = new FakeUserDao();
        FakeOtpTokenDao otpTokenDao = new FakeOtpTokenDao();
        CapturingEmailService emailService = new CapturingEmailService();
        UserServiceImpl service = new UserServiceImpl(userDao, otpTokenDao, emailService);
        User user = service.register(
                "student04",
                "fourth@example.com",
                "OriginalPass123!",
                "OriginalPass123!"
        );
        service.verifyOtp(user.getUserId(), OtpPurpose.REGISTER, emailService.lastCode);

        service.requestPasswordReset(user.getEmail());
        service.verifyOtp(user.getUserId(), OtpPurpose.RESET_PASSWORD, emailService.lastCode);
        assertTrue(otpTokenDao.latest.isVerified());
        assertFalse(otpTokenDao.latest.isUsed());

        service.resetPassword(user.getUserId(), "ReplacementPass123!", "ReplacementPass123!");

        assertTrue(otpTokenDao.latest.isUsed());
        assertThrows(
                IllegalArgumentException.class,
                () -> service.authenticate(user.getEmail(), "OriginalPass123!")
        );
        assertSame(user, service.authenticate(user.getEmail(), "ReplacementPass123!"));
        assertThrows(
                IllegalStateException.class,
                () -> service.resetPassword(
                        user.getUserId(),
                        "AnotherPass123!",
                        "AnotherPass123!"
                )
        );
    }

    private static User inactiveUser(FakeUserDao userDao, String username, String email) {
        User user = new User(username, email, "unused-test-hash");
        user.setActive(false);
        userDao.insert(user);
        return user;
    }

    private static OtpToken activeToken(User user, String code) {
        LocalDateTime sentAt = LocalDateTime.now();
        OtpToken token = new OtpToken();
        token.setUser(user);
        token.setPurpose(OtpPurpose.REGISTER);
        token.setCodeHash(OTPUtil.hash(code));
        token.setExpiresAt(OTPUtil.expiryFrom(sentAt));
        token.setAttemptsRemaining(OTPUtil.MAX_ATTEMPTS);
        token.setLastSentAt(sentAt);
        return token;
    }

    private static final class CapturingEmailService extends EmailService {

        private String lastCode;
        private OtpPurpose lastPurpose;

        private CapturingEmailService() {
            super("sender@example.com", "test-app-password");
        }

        @Override
        public void sendOtp(String recipient, String code, OtpPurpose purpose) {
            lastCode = code;
            lastPurpose = purpose;
        }
    }

    private static final class FakeOtpTokenDao implements IOtpTokenDao {

        private OtpToken latest;
        private long sequence;

        @Override
        public void replaceActive(OtpToken token) {
            if (latest != null && !latest.isUsed()) {
                latest.setUsedAt(LocalDateTime.now());
            }
            token.setOtpId(++sequence);
            latest = token;
        }

        @Override
        public void update(OtpToken token) {
            latest = token;
        }

        @Override
        public void activateUserAndConsume(OtpToken token, LocalDateTime completedAt) {
            token.setVerifiedAt(completedAt);
            token.setUsedAt(completedAt);
            token.getUser().setActive(true);
            latest = token;
        }

        @Override
        public void updatePasswordAndConsume(OtpToken token, String passwordHash,
                                             LocalDateTime completedAt) {
            token.getUser().setPasswordHash(passwordHash);
            token.setUsedAt(completedAt);
            latest = token;
        }

        @Override
        public OtpToken findLatest(int userId, OtpPurpose purpose) {
            if (latest == null || latest.getUser() == null
                    || !Integer.valueOf(userId).equals(latest.getUser().getUserId())
                    || purpose != latest.getPurpose()) {
                return null;
            }
            return latest;
        }
    }

    private static final class FakeUserDao implements IUserDao {

        private final List<User> users = new ArrayList<>();
        private int sequence;

        @Override
        public void insert(User user) {
            if (user.getUserId() == null) {
                user.setUserId(++sequence);
            }
            users.add(user);
        }

        @Override
        public void update(User user) {
            // Tests keep the same entity instance, so changes are already visible.
        }

        @Override
        public User findById(int userId) {
            return users.stream()
                    .filter(user -> Integer.valueOf(userId).equals(user.getUserId()))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public User findByUsername(String username) {
            return findByValue(username, false);
        }

        @Override
        public User findByEmail(String email) {
            return findByValue(email, true);
        }

        @Override
        public User findByUsernameOrEmail(String identifier) {
            User byUsername = findByUsername(identifier);
            return byUsername == null ? findByEmail(identifier) : byUsername;
        }

        private User findByValue(String value, boolean email) {
            if (value == null) {
                return null;
            }
            String expected = value.trim();
            return users.stream()
                    .filter(user -> expected.equalsIgnoreCase(
                            email ? user.getEmail() : user.getUsername()
                    ))
                    .findFirst()
                    .orElse(null);
        }
    }
}
