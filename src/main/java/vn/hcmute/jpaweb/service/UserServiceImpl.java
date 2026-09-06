package vn.hcmute.jpaweb.service;

import jakarta.persistence.EntityNotFoundException;
import vn.hcmute.jpaweb.dao.IOtpTokenDao;
import vn.hcmute.jpaweb.dao.IUserDao;
import vn.hcmute.jpaweb.dao.OtpTokenDao;
import vn.hcmute.jpaweb.dao.UserDao;
import vn.hcmute.jpaweb.entity.OtpPurpose;
import vn.hcmute.jpaweb.entity.OtpToken;
import vn.hcmute.jpaweb.entity.User;
import vn.hcmute.jpaweb.utils.OTPUtil;
import vn.hcmute.jpaweb.utils.PasswordUtil;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public class UserServiceImpl implements IUserService {

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 50;
    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]{3,50}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
                    + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$"
    );

    private final IUserDao userDao;
    private final IOtpTokenDao otpTokenDao;
    private final EmailService emailService;

    public UserServiceImpl() {
        this(new UserDao(), new OtpTokenDao(), new EmailService());
    }

    public UserServiceImpl(IUserDao userDao, IOtpTokenDao otpTokenDao, EmailService emailService) {
        this.userDao = Objects.requireNonNull(userDao, "User DAO must not be null");
        this.otpTokenDao = Objects.requireNonNull(otpTokenDao, "OTP token DAO must not be null");
        this.emailService = Objects.requireNonNull(emailService, "Email service must not be null");
    }

    @Override
    public User register(String username, String email, String password, String confirmPassword) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedEmail = normalizeEmail(email);
        validatePassword(password, confirmPassword);

        User usernameOwner = userDao.findByUsername(normalizedUsername);
        User emailOwner = userDao.findByEmail(normalizedEmail);
        if (usernameOwner == null && emailOwner == null) {
            emailService.requireConfigured();
            User user = new User(normalizedUsername, normalizedEmail, PasswordUtil.hash(password));
            user.setActive(false);
            userDao.insert(user);
            issueAndSendOtp(user, OtpPurpose.REGISTER);
            return user;
        }

        if (usernameOwner == null) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (emailOwner == null) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (!usernameOwner.getUserId().equals(emailOwner.getUserId())) {
            throw new IllegalArgumentException("Username and email already belong to different accounts");
        }
        if (usernameOwner.isActive()) {
            throw new IllegalArgumentException("Account already exists. Sign in instead");
        }

        emailService.requireConfigured();
        enforceResendCooldown(usernameOwner.getUserId(), OtpPurpose.REGISTER, LocalDateTime.now());
        usernameOwner.setPasswordHash(PasswordUtil.hash(password));
        userDao.update(usernameOwner);
        issueAndSendOtp(usernameOwner, OtpPurpose.REGISTER);
        return usernameOwner;
    }

    @Override
    public User authenticate(String identifier, String password) {
        if (identifier == null || identifier.isBlank() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Username/email and password are required");
        }

        User user = userDao.findByUsernameOrEmail(identifier.trim());
        if (user == null || !PasswordUtil.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username/email or password");
        }
        if (!user.isActive()) {
            throw new IllegalStateException("Account is not active. Verify the registration OTP first");
        }
        return user;
    }

    @Override
    public User requestPasswordReset(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userDao.findByEmail(normalizedEmail);
        if (user == null) {
            throw new EntityNotFoundException("No account is registered with this email");
        }
        if (!user.isActive()) {
            throw new IllegalStateException("Account is not active");
        }

        emailService.requireConfigured();
        enforceResendCooldown(user.getUserId(), OtpPurpose.RESET_PASSWORD, LocalDateTime.now());
        issueAndSendOtp(user, OtpPurpose.RESET_PASSWORD);
        return user;
    }

    @Override
    public void verifyOtp(int userId, OtpPurpose purpose, String code) {
        validateUserId(userId);
        Objects.requireNonNull(purpose, "OTP purpose must not be null");
        User user = requireUser(userId);
        validateAccountState(user, purpose);

        OtpToken token = requireUsableToken(userId, purpose);
        LocalDateTime now = LocalDateTime.now();
        if (token.isExpired(now)) {
            token.setUsedAt(now);
            otpTokenDao.update(token);
            throw new IllegalArgumentException("OTP has expired. Request a new code");
        }
        if (token.getAttemptsRemaining() <= 0) {
            if (!token.isUsed()) {
                token.setUsedAt(now);
                otpTokenDao.update(token);
            }
            throw new IllegalStateException("OTP attempt limit reached. Request a new code");
        }
        if (!OTPUtil.matches(code, token.getCodeHash())) {
            int attemptsRemaining = token.getAttemptsRemaining() - 1;
            token.setAttemptsRemaining(Math.max(0, attemptsRemaining));
            if (attemptsRemaining <= 0) {
                token.setUsedAt(now);
            }
            otpTokenDao.update(token);
            if (attemptsRemaining <= 0) {
                throw new IllegalStateException("OTP attempt limit reached. Request a new code");
            }
            throw new IllegalArgumentException(
                    "Invalid OTP. " + attemptsRemaining + " attempt(s) remaining"
            );
        }

        if (purpose == OtpPurpose.REGISTER) {
            otpTokenDao.activateUserAndConsume(token, now);
            user.setActive(true);
            return;
        }
        if (token.getVerifiedAt() == null) {
            token.setVerifiedAt(now);
        }
        otpTokenDao.update(token);
    }

    @Override
    public void resendOtp(int userId, OtpPurpose purpose) {
        validateUserId(userId);
        Objects.requireNonNull(purpose, "OTP purpose must not be null");
        User user = requireUser(userId);
        validateAccountState(user, purpose);
        emailService.requireConfigured();
        enforceResendCooldown(userId, purpose, LocalDateTime.now());
        issueAndSendOtp(user, purpose);
    }

    @Override
    public void resetPassword(int userId, String password, String confirmPassword) {
        validateUserId(userId);
        validatePassword(password, confirmPassword);
        User user = requireUser(userId);
        if (!user.isActive()) {
            throw new IllegalStateException("Account is not active");
        }

        OtpToken token = otpTokenDao.findLatest(userId, OtpPurpose.RESET_PASSWORD);
        LocalDateTime now = LocalDateTime.now();
        if (token == null || token.isUsed() || !token.isVerified()) {
            throw new IllegalStateException("Password reset OTP has not been verified");
        }
        LocalDateTime authorizationExpiresAt = OTPUtil.resetAuthorizationExpiryFrom(token.getVerifiedAt());
        if (!authorizationExpiresAt.isAfter(now)) {
            token.setUsedAt(now);
            otpTokenDao.update(token);
            throw new IllegalStateException("Password reset authorization has expired");
        }

        String passwordHash = PasswordUtil.hash(password);
        otpTokenDao.updatePasswordAndConsume(token, passwordHash, now);
        user.setPasswordHash(passwordHash);
    }

    @Override
    public User findById(int userId) {
        validateUserId(userId);
        return userDao.findById(userId);
    }

    private void issueAndSendOtp(User user, OtpPurpose purpose) {
        emailService.requireConfigured();
        LocalDateTime sentAt = LocalDateTime.now();
        String code = OTPUtil.generateCode();

        OtpToken token = new OtpToken();
        token.setUser(user);
        token.setPurpose(purpose);
        token.setCodeHash(OTPUtil.hash(code));
        token.setExpiresAt(OTPUtil.expiryFrom(sentAt));
        token.setAttemptsRemaining(OTPUtil.MAX_ATTEMPTS);
        token.setLastSentAt(sentAt);
        otpTokenDao.replaceActive(token);

        try {
            emailService.sendOtp(user.getEmail(), code, purpose);
        } catch (RuntimeException exception) {
            token.setUsedAt(LocalDateTime.now());
            try {
                otpTokenDao.update(token);
            } catch (RuntimeException invalidationException) {
                exception.addSuppressed(invalidationException);
            }
            throw new EmailDeliveryException(
                    user.getUserId(),
                    purpose,
                    "OTP was created, but the email could not be sent. Check the Gmail SMTP configuration.",
                    exception
            );
        }
    }

    private void enforceResendCooldown(int userId, OtpPurpose purpose, LocalDateTime now) {
        OtpToken latest = otpTokenDao.findLatest(userId, purpose);
        if (latest != null && !latest.isUsed()
                && !OTPUtil.canResend(latest.getLastSentAt(), now)) {
            long seconds = java.time.Duration.between(
                    now,
                    latest.getLastSentAt().plus(OTPUtil.RESEND_COOLDOWN)
            ).toSeconds();
            throw new IllegalStateException(
                    "Please wait " + Math.max(1, seconds) + " second(s) before requesting another OTP"
            );
        }
    }

    private OtpToken requireUsableToken(int userId, OtpPurpose purpose) {
        OtpToken token = otpTokenDao.findLatest(userId, purpose);
        if (token == null || token.isUsed()) {
            throw new IllegalStateException("No active OTP. Request a new code");
        }
        return token;
    }

    private User requireUser(int userId) {
        User user = userDao.findById(userId);
        if (user == null) {
            throw new EntityNotFoundException("User not found: " + userId);
        }
        return user;
    }

    private static void validateAccountState(User user, OtpPurpose purpose) {
        if (purpose == OtpPurpose.REGISTER && user.isActive()) {
            throw new IllegalStateException("Account is already active");
        }
        if (purpose == OtpPurpose.RESET_PASSWORD && !user.isActive()) {
            throw new IllegalStateException("Account is not active");
        }
    }

    private static String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        String normalized = username.trim();
        if (normalized.length() < MIN_USERNAME_LENGTH || normalized.length() > MAX_USERNAME_LENGTH
                || !USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Username must be 3-50 characters and contain only letters, digits, dot, underscore, or hyphen"
            );
        }
        return normalized;
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Email address is not valid");
        }
        return normalized;
    }

    private static void validatePassword(String password, String confirmPassword) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must contain at least 8 characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES) {
            throw new IllegalArgumentException("Password must not exceed 72 UTF-8 bytes");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Password confirmation does not match");
        }
    }

    private static void validateUserId(int userId) {
        if (userId < 1) {
            throw new IllegalArgumentException("User ID must be greater than 0");
        }
    }
}
