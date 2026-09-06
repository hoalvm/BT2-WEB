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

@WebServlet(name = "RegisterController", urlPatterns = "/register")
public class RegisterController extends HttpServlet {

    private static final String VIEW = "/views/auth/register.jsp";
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
        String username = trimToEmpty(request.getParameter("username"));
        String email = trimToEmpty(request.getParameter("email"));
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        request.setAttribute("username", username);
        request.setAttribute("email", email);

        try {
            User user = userService.register(username, email, password, confirmPassword);
            HttpSession session = request.getSession(true);
            AuthControllerSupport.setPendingOtp(session, user, OtpPurpose.REGISTER);
            session.setAttribute("infoMessage", "We sent a 6-digit code to your email.");
            response.sendRedirect(request.getContextPath() + "/verify-otp");
        } catch (EmailDeliveryException exception) {
            if (restorePendingUser(request, exception.getUserId(), OtpPurpose.REGISTER)) {
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
                    AuthControllerSupport.safeMessage(exception, "Unable to create the account."));
            AuthControllerSupport.forward(request, response, VIEW);
        } catch (RuntimeException exception) {
            getServletContext().log("Registration failed.", exception);
            request.setAttribute("errorMessage", "Unable to create the account. Please try again.");
            AuthControllerSupport.forward(request, response, VIEW);
        }
    }

    private boolean restorePendingUser(HttpServletRequest request, Integer userId, OtpPurpose purpose) {
        if (userId == null || userId <= 0) {
            return false;
        }
        User user = userService.findById(userId);
        if (user == null) {
            return false;
        }
        AuthControllerSupport.setPendingOtp(request.getSession(true), user, purpose);
        return true;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
