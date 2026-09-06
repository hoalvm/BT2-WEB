package vn.hcmute.jpaweb.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.hcmute.jpaweb.service.IUserService;
import vn.hcmute.jpaweb.service.UserServiceImpl;

import java.io.IOException;

@WebServlet(name = "ResetPasswordController", urlPatterns = "/reset-password")
public class ResetPasswordController extends HttpServlet {

    private static final String VIEW = "/views/auth/reset-password.jsp";
    private transient IUserService userService;

    @Override
    public void init() {
        userService = new UserServiceImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AuthControllerSupport.prepare(request, response);
        if (!requireResetGrant(request, response)) {
            return;
        }
        AuthControllerSupport.consumeFlash(request);
        AuthControllerSupport.forward(request, response, VIEW);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AuthControllerSupport.prepare(request, response);
        if (!requireResetGrant(request, response)) {
            return;
        }
        HttpSession session = request.getSession(false);
        Integer userId = AuthControllerSupport.getInteger(session, AuthControllerSupport.RESET_USER_ID);
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        try {
            userService.resetPassword(userId, password, confirmPassword);
            AuthControllerSupport.clearResetGrant(session);
            session.setAttribute("successMessage", "Password updated. You can sign in now.");
            response.sendRedirect(request.getContextPath() + "/login");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            request.setAttribute("errorMessage",
                    AuthControllerSupport.safeMessage(exception, "Unable to update the password."));
            AuthControllerSupport.forward(request, response, VIEW);
        } catch (RuntimeException exception) {
            getServletContext().log("Password reset failed.", exception);
            request.setAttribute("errorMessage", "Unable to update the password. Please try again.");
            AuthControllerSupport.forward(request, response, VIEW);
        }
    }

    private boolean requireResetGrant(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (AuthControllerSupport.hasValidResetGrant(session)) {
            return true;
        }
        AuthControllerSupport.clearResetGrant(session);
        AuthControllerSupport.setFlash(request, "errorMessage", "Verify a password reset code first.");
        response.sendRedirect(request.getContextPath() + "/forgot-password");
        return false;
    }
}
