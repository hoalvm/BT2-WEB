package vn.hcmute.jpaweb.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.hcmute.jpaweb.entity.OtpPurpose;
import vn.hcmute.jpaweb.entity.User;
import vn.hcmute.jpaweb.service.EmailDeliveryException;
import vn.hcmute.jpaweb.service.IUserService;
import vn.hcmute.jpaweb.service.UserServiceImpl;

import java.io.IOException;
import java.time.LocalDateTime;

@WebServlet(
        name = "VerifyOtpController",
        urlPatterns = {"/verify-otp", "/verify-otp/resend"}
)
public class VerifyOtpController extends HttpServlet {

    private static final String VIEW = "/views/auth/verify-otp.jsp";
    private transient IUserService userService;

    @Override
    public void init() {
        userService = new UserServiceImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AuthControllerSupport.prepare(request, response);
        AuthControllerSupport.consumeFlash(request);
        if (!prepareVerificationPage(request, response)) {
            return;
        }
        AuthControllerSupport.forward(request, response, VIEW);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AuthControllerSupport.prepare(request, response);
        if ("/verify-otp/resend".equals(request.getServletPath())) {
            resend(request, response);
        } else {
            verify(request, response);
        }
    }

    private void verify(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Integer userId = AuthControllerSupport.getInteger(session, AuthControllerSupport.PENDING_USER_ID);
        OtpPurpose purpose = AuthControllerSupport.getPendingPurpose(session);
        if (userId == null || purpose == null) {
            redirectToStart(request, response, purpose, "Start the verification flow again.");
            return;
        }

        String code = request.getParameter("otp");
        try {
            userService.verifyOtp(userId, purpose, code);
            AuthControllerSupport.clearPendingOtp(session);
            if (purpose == OtpPurpose.REGISTER) {
                session.setAttribute("successMessage", "Account activated. You can sign in now.");
                response.sendRedirect(request.getContextPath() + "/login");
            } else {
                session.setAttribute(AuthControllerSupport.RESET_USER_ID, userId);
                session.setAttribute(AuthControllerSupport.RESET_EXPIRES_AT, LocalDateTime.now().plusMinutes(10));
                response.sendRedirect(request.getContextPath() + "/reset-password");
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            request.setAttribute("errorMessage",
                    AuthControllerSupport.safeMessage(exception, "The verification code is invalid."));
            if (prepareVerificationPage(request, response)) {
                AuthControllerSupport.forward(request, response, VIEW);
            }
        } catch (RuntimeException exception) {
            getServletContext().log("OTP verification failed.", exception);
            request.setAttribute("errorMessage", "Unable to verify the code. Please try again.");
            if (prepareVerificationPage(request, response)) {
                AuthControllerSupport.forward(request, response, VIEW);
            }
        }
    }

    private void resend(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Integer userId = AuthControllerSupport.getInteger(session, AuthControllerSupport.PENDING_USER_ID);
        OtpPurpose purpose = AuthControllerSupport.getPendingPurpose(session);
        if (userId == null || purpose == null) {
            redirectToStart(request, response, purpose, "Start the verification flow again.");
            return;
        }

        try {
            userService.resendOtp(userId, purpose);
            AuthControllerSupport.setFlash(request, "successMessage", "A new code has been sent.");
            response.sendRedirect(request.getContextPath() + "/verify-otp");
        } catch (EmailDeliveryException exception) {
            request.setAttribute("errorMessage",
                    AuthControllerSupport.safeMessage(exception, "Email delivery failed. Please try again."));
            if (prepareVerificationPage(request, response)) {
                AuthControllerSupport.forward(request, response, VIEW);
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            request.setAttribute("errorMessage",
                    AuthControllerSupport.safeMessage(exception, "Unable to resend the code."));
            if (prepareVerificationPage(request, response)) {
                AuthControllerSupport.forward(request, response, VIEW);
            }
        } catch (RuntimeException exception) {
            getServletContext().log("OTP resend failed.", exception);
            request.setAttribute("errorMessage", "Unable to resend the code. Please try again.");
            if (prepareVerificationPage(request, response)) {
                AuthControllerSupport.forward(request, response, VIEW);
            }
        }
    }

    private boolean prepareVerificationPage(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        Integer userId = AuthControllerSupport.getInteger(session, AuthControllerSupport.PENDING_USER_ID);
        OtpPurpose purpose = AuthControllerSupport.getPendingPurpose(session);
        if (userId == null || purpose == null) {
            redirectToStart(request, response, purpose, "Start the verification flow again.");
            return false;
        }
        User user = userService.findById(userId);
        if (user == null) {
            AuthControllerSupport.clearPendingOtp(session);
            redirectToStart(request, response, purpose, "The account no longer exists.");
            return false;
        }
        request.setAttribute("purpose", purpose.name());
        request.setAttribute("email", maskEmail(user.getEmail()));
        return true;
    }

    private void redirectToStart(HttpServletRequest request, HttpServletResponse response,
                                 OtpPurpose purpose, String message) throws IOException {
        AuthControllerSupport.setFlash(request, "errorMessage", message);
        String path = purpose == OtpPurpose.RESET_PASSWORD ? "/forgot-password" : "/register";
        response.sendRedirect(request.getContextPath() + path);
    }

    private String maskEmail(String email) {
        if (email == null || email.indexOf('@') <= 0) {
            return "your email";
        }
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String visible = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return visible + "***" + email.substring(at);
    }
}
