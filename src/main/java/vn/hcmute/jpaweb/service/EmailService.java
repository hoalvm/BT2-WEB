package vn.hcmute.jpaweb.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import vn.hcmute.jpaweb.entity.OtpPurpose;
import vn.hcmute.jpaweb.utils.Constants;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class EmailService {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String UTF_8 = StandardCharsets.UTF_8.name();

    private final String username;
    private final String appPassword;

    public EmailService() {
    this(
            "thanhnhatcyber@gmail.com",
            "azynqqiddkcwtdao"
    );
}

    public EmailService(String username, String appPassword) {
        this.username = trimToNull(username);
        this.appPassword = trimToNull(appPassword);
    }

    public boolean isConfigured() {
        return username != null && appPassword != null;
    }

    public void requireConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "Gmail SMTP is not configured. Set JPAWEB_MAIL_USERNAME and JPAWEB_MAIL_APP_PASSWORD."
            );
        }
    }

    public void sendOtp(String recipient, String code, OtpPurpose purpose) {
        requireConfigured();
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("Email recipient must not be empty");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("OTP code must not be empty");
        }
        if (purpose == null) {
            throw new IllegalArgumentException("OTP purpose must not be null");
        }

        Properties properties = new Properties();
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.smtp.starttls.enable", "true");
        properties.setProperty("mail.smtp.starttls.required", "true");
        properties.setProperty("mail.smtp.host", SMTP_HOST);
        properties.setProperty("mail.smtp.port", SMTP_PORT);
        properties.setProperty("mail.smtp.connectiontimeout", "10000");
        properties.setProperty("mail.smtp.timeout", "10000");
        properties.setProperty("mail.smtp.writetimeout", "10000");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, appPassword);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient.trim()));
            message.setSubject(subjectFor(purpose), UTF_8);
            message.setText(bodyFor(code, purpose), UTF_8, "html");
            Transport.send(message);
        } catch (MessagingException exception) {
            throw new IllegalStateException("Unable to send OTP email through Gmail SMTP", exception);
        }
    }

    private static String subjectFor(OtpPurpose purpose) {
        return purpose == OtpPurpose.REGISTER
                ? "Activate your JPA Web account"
                : "Reset your JPA Web password";
    }

    private static String bodyFor(String code, OtpPurpose purpose) {
        String action = purpose == OtpPurpose.REGISTER
                ? "activate your account"
                : "reset your password";
        return "<p>Use the following one-time password to " + action + ":</p>"
                + "<p style=\"font-size:24px;font-weight:bold;letter-spacing:4px\">" + code + "</p>"
                + "<p>This code expires in 5 minutes and can be attempted up to 5 times.</p>"
                + "<p>If you did not request this code, you can ignore this email.</p>";
    }

    private static String resolveSetting(String systemProperty, String environmentVariable) {
        String value = System.getProperty(systemProperty);
        if (value == null || value.isBlank()) {
            value = System.getenv(environmentVariable);
        }
        return trimToNull(value);
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
