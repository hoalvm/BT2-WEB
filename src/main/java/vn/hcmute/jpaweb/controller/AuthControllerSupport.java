package vn.hcmute.jpaweb.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.hcmute.jpaweb.entity.OtpPurpose;
import vn.hcmute.jpaweb.entity.User;

import java.io.IOException;
import java.time.LocalDateTime;

final class AuthControllerSupport {

    static final String CURRENT_USER = "currentUser";
    static final String PENDING_USER_ID = "pendingOtpUserId";
    static final String PENDING_PURPOSE = "pendingOtpPurpose";
    static final String RESET_USER_ID = "passwordResetUserId";
    static final String RESET_EXPIRES_AT = "passwordResetExpiresAt";
    static final String LOGIN_REDIRECT = "loginRedirect";

    private AuthControllerSupport() {
    }

    static void prepare(HttpServletRequest request, HttpServletResponse response)
            throws java.io.UnsupportedEncodingException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
    }

    static void forward(HttpServletRequest request, HttpServletResponse response, String view)
            throws ServletException, IOException {
        RequestDispatcher dispatcher = request.getRequestDispatcher(view);
        dispatcher.forward(request, response);
    }

    static void setFlash(HttpServletRequest request, String key, String message) {
        request.getSession(true).setAttribute(key, message);
    }

    static void consumeFlash(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        moveFlash(session, request, "successMessage");
        moveFlash(session, request, "errorMessage");
        moveFlash(session, request, "infoMessage");
    }

    private static void moveFlash(HttpSession session, HttpServletRequest request, String key) {
        Object message = session.getAttribute(key);
        if (message != null) {
            request.setAttribute(key, message);
            session.removeAttribute(key);
        }
    }

    static void setPendingOtp(HttpSession session, User user, OtpPurpose purpose) {
        session.setAttribute(PENDING_USER_ID, user.getUserId());
        session.setAttribute(PENDING_PURPOSE, purpose);
    }

    static Integer getInteger(HttpSession session, String key) {
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(key);
        return value instanceof Integer integer ? integer : null;
    }

    static OtpPurpose getPendingPurpose(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(PENDING_PURPOSE);
        if (value instanceof OtpPurpose purpose) {
            return purpose;
        }
        if (value instanceof String text) {
            try {
                return OtpPurpose.valueOf(text);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    static void clearPendingOtp(HttpSession session) {
        if (session != null) {
            session.removeAttribute(PENDING_USER_ID);
            session.removeAttribute(PENDING_PURPOSE);
        }
    }

    static boolean hasValidResetGrant(HttpSession session) {
        if (session == null || getInteger(session, RESET_USER_ID) == null) {
            return false;
        }
        Object expiresAt = session.getAttribute(RESET_EXPIRES_AT);
        return expiresAt instanceof LocalDateTime dateTime && dateTime.isAfter(LocalDateTime.now());
    }

    static void clearResetGrant(HttpSession session) {
        if (session != null) {
            session.removeAttribute(RESET_USER_ID);
            session.removeAttribute(RESET_EXPIRES_AT);
        }
    }

    static String safeMessage(RuntimeException exception, String fallback) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }
}
