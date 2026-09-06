package vn.hcmute.jpaweb.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.hcmute.jpaweb.entity.Product;
import vn.hcmute.jpaweb.service.IProductService;
import vn.hcmute.jpaweb.service.ProductServiceImpl;
import vn.hcmute.jpaweb.utils.Constants;

import java.io.IOException;
import java.util.Collections;

@WebServlet(
        name = "ProductPublicController",
        urlPatterns = {"/product", "/product/detail"}
)
public class ProductPublicController extends HttpServlet {

    private static final String LIST_VIEW = "/views/product.jsp";
    private static final String DETAIL_VIEW = "/views/product-detail.jsp";
    private static final int PAGE_SIZE = Constants.PRODUCT_PAGE_SIZE;

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

        switch (request.getServletPath()) {
            case "/product" -> showProductList(request, response);
            case "/product/detail" -> showProductDetail(request, response);
            default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void showProductList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int requestedPage = parsePage(request.getParameter("page"));

        try {
            int totalItems = productService.countActive();
            int totalPages = totalItems == 0 ? 0 : (totalItems + PAGE_SIZE - 1) / PAGE_SIZE;
            int currentPage = totalPages == 0 ? 1 : Math.min(requestedPage, totalPages);

            request.setAttribute("products", productService.findAllActive(currentPage, PAGE_SIZE));
            request.setAttribute("currentPage", currentPage);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("totalItems", totalItems);
        } catch (RuntimeException exception) {
            getServletContext().log("Unable to load the public product catalog.", exception);
            request.setAttribute("products", Collections.emptyList());
            request.setAttribute("currentPage", 1);
            request.setAttribute("totalPages", 0);
            request.setAttribute("totalItems", 0);
            request.setAttribute("errorMessage",
                    "Products are temporarily unavailable. Please try again later.");
        }

        request.setAttribute("pageSize", PAGE_SIZE);
        forward(request, response, LIST_VIEW);
    }

    private void showProductDetail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer productId = parsePositiveId(request.getParameter("id"));
        if (productId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid product ID.");
            return;
        }

        try {
            Product product = productService.findActiveById(productId);
            if (product == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Product not found.");
                return;
            }
            request.setAttribute("product", product);
            forward(request, response, DETAIL_VIEW);
        } catch (EntityNotFoundException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Product not found.");
        } catch (RuntimeException exception) {
            getServletContext().log("Unable to load product detail.", exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to load the product.");
        }
    }

    private int parsePage(String rawPage) {
        Integer page = parsePositiveId(rawPage);
        return page == null ? 1 : page;
    }

    private Integer parsePositiveId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }
        try {
            int id = Integer.parseInt(rawId.trim());
            return id > 0 ? id : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void forward(HttpServletRequest request, HttpServletResponse response, String view)
            throws ServletException, IOException {
        RequestDispatcher dispatcher = request.getRequestDispatcher(view);
        dispatcher.forward(request, response);
    }
}
