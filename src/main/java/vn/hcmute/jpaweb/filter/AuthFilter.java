package vn.hcmute.jpaweb.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.hcmute.jpaweb.entity.User;

import java.io.IOException;

@WebFilter(filterName = "AuthFilter", urlPatterns = {"/admin/*", "/views/admin/*"})
public class AuthFilter implements Filter {

    private static final String CURRENT_USER = "currentUser";
    private static final String LOGIN_REDIRECT = "loginRedirect";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        HttpSession session = request.getSession(false);
        Object sessionUser = session == null ? null : session.getAttribute(CURRENT_USER);

        if (sessionUser instanceof User user && user.isActive()) {
            chain.doFilter(request, response);
            return;
        }

        if ("GET".equalsIgnoreCase(request.getMethod())) {
            String returnUrl = request.getRequestURI();
            if (request.getQueryString() != null && !request.getQueryString().isBlank()) {
                returnUrl += "?" + request.getQueryString();
            }
            request.getSession(true).setAttribute(LOGIN_REDIRECT, returnUrl);
        }
        response.sendRedirect(request.getContextPath() + "/login");
    }
}
