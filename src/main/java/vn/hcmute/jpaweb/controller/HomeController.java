package vn.hcmute.jpaweb.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.hcmute.jpaweb.service.IProductService;
import vn.hcmute.jpaweb.service.ProductServiceImpl;
import vn.hcmute.jpaweb.utils.Constants;

import java.io.IOException;
import java.util.Collections;

@WebServlet(name = "HomeController", urlPatterns = "/home")
public class HomeController extends HttpServlet {

    private static final String HOME_VIEW = "/views/home.jsp";
    private static final int NEWEST_PRODUCT_LIMIT = Constants.HOME_PRODUCT_LIMIT;

    private transient IProductService productService;

    @Override
    public void init() {
        productService = new ProductServiceImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        try {
            request.setAttribute("products", productService.findNewestActive(NEWEST_PRODUCT_LIMIT));
        } catch (RuntimeException exception) {
            getServletContext().log("Unable to load newest products.", exception);
            request.setAttribute("products", Collections.emptyList());
            request.setAttribute("errorMessage",
                    "Newest products are temporarily unavailable. Please try again later.");
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher(HOME_VIEW);
        dispatcher.forward(request, response);
    }
}
