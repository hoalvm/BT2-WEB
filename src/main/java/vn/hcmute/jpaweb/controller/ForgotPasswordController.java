package vn.hcmute.jpaweb.controller;

import jakarta.persistence.EntityNotFoundException;
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

@WebServlet(name = "ForgotPasswordController", urlPatterns = "/forgot-password")
public class ForgotPasswordController extends HttpServlet {

    private static final String VIEW = "/views/auth/forgot-password.jsp";
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
        AuthControllerSupport.forward(request, response, VIEW);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AuthControllerSupport.prepare(request, response);
        String email = trimToEmpty(request.getParameter("email"));
        request.setAttribute("email", email);

        try {
            User user = userService.requestPasswordReset(email);
            startVerification(request, user);
            request.getSession().setAttribute("infoMessage", "We sent a password reset code to your email.");
            response.sendRedirect(request.getContextPath() + "/verify-otp");
        } catch (EmailDeliveryException exception) {
            User user = exception.getUserId() > 0 ? userService.findById(exception.getUserId()) : null;
            if (user != null) {
                startVerification(request, user);
                AuthControllerSupport.setFlash(request, "errorMessage",
                        AuthControllerSupport.safeMessage(exception, "Email delivery failed. Please resend the code."));
                response.sendRedirect(request.getContextPath() + "/verify-otp");
            } else {
                request.setAttribute("errorMessage",
                        AuthControllerSupport.safeMessage(exception, "Email delivery failed. Please try again."));
                AuthControllerSupport.forward(request, response, VIEW);
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            request.setAttribute("errorMessage",
                    AuthControllerSupport.safeMessage(exception, "Unable to start password reset."));
            AuthControllerSupport.forward(request, response, VIEW);
        } catch (EntityNotFoundException exception) {
            request.setAttribute("errorMessage",
                    AuthControllerSupport.safeMessage(exception, "No account is registered with this email."));
            AuthControllerSupport.forward(request, response, VIEW);
        } catch (RuntimeException exception) {
            getServletContext().log("Password reset request failed.", exception);
            request.setAttribute("errorMessage", "Unable to start password reset. Please try again.");
            AuthControllerSupport.forward(request, response, VIEW);
        }
    }

    private void startVerification(HttpServletRequest request, User user) {
        HttpSession session = request.getSession(true);
        AuthControllerSupport.clearResetGrant(session);
        AuthControllerSupport.setPendingOtp(session, user, OtpPurpose.RESET_PASSWORD);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
