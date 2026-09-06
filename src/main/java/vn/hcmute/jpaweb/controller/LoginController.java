package vn.hcmute.jpaweb.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.hcmute.jpaweb.entity.User;
import vn.hcmute.jpaweb.service.IUserService;
import vn.hcmute.jpaweb.service.UserServiceImpl;

import java.io.IOException;

@WebServlet(name = "LoginController", urlPatterns = "/login")
public class LoginController extends HttpServlet {

    private static final String VIEW = "/views/auth/login.jsp";
    private transient IUserService userService;

    @Override
    public void init() {
        userService = new UserServiceImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AuthControllerSupport.prepare(request, response);
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(AuthControllerSupport.CURRENT_USER) instanceof User user
                && user.isActive()) {
            response.sendRedirect(request.getContextPath() + "/home");
            return;
        }
        AuthControllerSupport.consumeFlash(request);
        AuthControllerSupport.forward(request, response, VIEW);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AuthControllerSupport.prepare(request, response);
        String identifier = trimToEmpty(request.getParameter("identifier"));
        String password = request.getParameter("password");
        request.setAttribute("identifier", identifier);

        try {
            User user = userService.authenticate(identifier, password);
            HttpSession session = request.getSession(true);
            request.changeSessionId();
            session.setAttribute(AuthControllerSupport.CURRENT_USER, user);

            String redirect = session.getAttribute(AuthControllerSupport.LOGIN_REDIRECT) instanceof String value
                    ? value : null;
            session.removeAttribute(AuthControllerSupport.LOGIN_REDIRECT);
            if (!isSafeAdminRedirect(redirect, request.getContextPath())) {
                redirect = request.getContextPath() + "/home";
            }
            response.sendRedirect(redirect);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            request.setAttribute("errorMessage",
                    AuthControllerSupport.safeMessage(exception, "Unable to sign in."));
            AuthControllerSupport.forward(request, response, VIEW);
        } catch (RuntimeException exception) {
            getServletContext().log("Login failed.", exception);
            request.setAttribute("errorMessage", "Unable to sign in. Please try again.");
            AuthControllerSupport.forward(request, response, VIEW);
        }
    }

    private boolean isSafeAdminRedirect(String redirect, String contextPath) {
        return redirect != null
                && redirect.startsWith(contextPath + "/admin/")
                && !redirect.contains("\r")
                && !redirect.contains("\n");
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
