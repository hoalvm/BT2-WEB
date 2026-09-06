package vn.hcmute.jpaweb.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import vn.hcmute.jpaweb.entity.Category;
import vn.hcmute.jpaweb.entity.Product;
import vn.hcmute.jpaweb.service.CategoryServiceImpl;
import vn.hcmute.jpaweb.service.ICategoryService;
import vn.hcmute.jpaweb.service.IProductService;
import vn.hcmute.jpaweb.service.ProductServiceImpl;
import vn.hcmute.jpaweb.utils.Constants;
import vn.hcmute.jpaweb.utils.ImageStorage;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@WebServlet(
        name = "ProductController",
        urlPatterns = {
                "/admin/products",
                "/admin/product/add",
                "/admin/product/insert",
                "/admin/product/edit",
                "/admin/product/update",
                "/admin/product/delete"
        }
)
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = Constants.MAX_UPLOAD_BYTES,
        maxRequestSize = Constants.MAX_UPLOAD_BYTES + 1024 * 1024
)
public class ProductController extends HttpServlet {

    private static final String LIST_VIEW = "/views/admin/product/product-list.jsp";
    private static final String ADD_VIEW = "/views/admin/product/product-add.jsp";
    private static final String EDIT_VIEW = "/views/admin/product/product-edit.jsp";

    private transient IProductService productService;
    private transient ICategoryService categoryService;
    private transient ImageStorage imageStorage;

    @Override
    public void init() throws ServletException {
        productService = new ProductServiceImpl();
        categoryService = new CategoryServiceImpl();
        imageStorage = new ImageStorage();
        try {
            imageStorage.ensureUploadDirectory();
        } catch (IOException exception) {
            throw new UnavailableException("Unable to initialize the image upload directory.");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        prepareEncoding(request, response);

        switch (request.getServletPath()) {
            case "/admin/products" -> showProductList(request, response);
            case "/admin/product/add" -> showAddForm(request, response);
            case "/admin/product/edit" -> showEditForm(request, response);
            case "/admin/product/insert", "/admin/product/update", "/admin/product/delete" ->
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        prepareEncoding(request, response);

        String servletPath = request.getServletPath();
        try {
            switch (servletPath) {
                case "/admin/product/insert" -> insertProduct(request, response);
                case "/admin/product/update" -> updateProduct(request, response);
                case "/admin/product/delete" -> deleteProduct(request, response);
                default -> response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
        } catch (IllegalStateException exception) {
            setFlash(request, "errorMessage", "The uploaded image must not exceed 5 MB.");
            if ("/admin/product/insert".equals(servletPath)) {
                response.sendRedirect(request.getContextPath() + "/admin/product/add");
            } else {
                redirectToList(request, response);
            }
        }
    }

    private void showProductList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlashMessages(request);
        try {
            List<Product> products = productService.findAll();
            request.setAttribute("products", products);
            request.setAttribute("totalItems", products.size());
        } catch (RuntimeException exception) {
            getServletContext().log("Unable to load products.", exception);
            request.setAttribute("products", Collections.emptyList());
            request.setAttribute("totalItems", 0);
            if (request.getAttribute("errorMessage") == null) {
                request.setAttribute("errorMessage",
                        "Unable to load products. Check the database connection and try again.");
            }
        }
        forward(request, response, LIST_VIEW);
    }

    private void showAddForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlashMessages(request);
        Product product = new Product();
        product.setStatus(1);
        request.setAttribute("product", product);
        loadCategories(request);
        forward(request, response, ADD_VIEW);
    }

    private void insertProduct(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Product product = new Product();
        product.setStatus(1);
        request.setAttribute("product", product);

        String rawImageUrl = trimToEmpty(request.getParameter("imageUrl"));
        request.setAttribute("imageUrl", rawImageUrl);
        request.setAttribute("categoryId", trimToEmpty(request.getParameter("categoryId")));

        try {
            populateProduct(request, product);
        } catch (IllegalArgumentException exception) {
            showFormError(request, response, ADD_VIEW, exception.getMessage());
            return;
        }

        Part imagePart;
        try {
            imagePart = request.getPart("imagesFile");
        } catch (IllegalStateException exception) {
            showFormError(request, response, ADD_VIEW,
                    "The uploaded image must not exceed 5 MB.");
            return;
        } catch (IOException | ServletException exception) {
            getServletContext().log("Unable to read the product image upload.", exception);
            showFormError(request, response, ADD_VIEW, "Unable to read the selected image.");
            return;
        }

        String newLocalImage = null;
        try {
            if (ImageStorage.hasUpload(imagePart)) {
                newLocalImage = imageStorage.save(imagePart);
                product.setImages(newLocalImage);
            } else if (!rawImageUrl.isBlank()) {
                product.setImages(validateRemoteImageUrl(rawImageUrl));
            } else {
                product.setImages(Constants.DEFAULT_IMAGE);
            }

            productService.insert(product);
            setFlash(request, "successMessage", "Product added successfully.");
            redirectToList(request, response);
        } catch (IllegalArgumentException exception) {
            deleteQuietly(newLocalImage);
            product.setImages(Constants.DEFAULT_IMAGE);
            showFormError(request, response, ADD_VIEW, exception.getMessage());
        } catch (RuntimeException | IOException exception) {
            deleteQuietly(newLocalImage);
            getServletContext().log("Unable to add the product.", exception);
            product.setImages(Constants.DEFAULT_IMAGE);
            showFormError(request, response, ADD_VIEW,
                    "Unable to add the product. Please try again.");
        }
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer productId = parsePositiveId(request.getParameter("id"));
        if (productId == null) {
            setFlash(request, "errorMessage", "Invalid product ID.");
            redirectToList(request, response);
            return;
        }

        try {
            Product product = productService.findById(productId);
            if (product == null) {
                setFlash(request, "errorMessage", "Product not found.");
                redirectToList(request, response);
                return;
            }
            consumeFlashMessages(request);
            request.setAttribute("product", product);
            if (product.getCategory() != null) {
                request.setAttribute("categoryId", product.getCategory().getCategoryId());
            }
            if (ImageStorage.isRemoteImage(product.getImages())) {
                request.setAttribute("imageUrl", product.getImages());
            }
            loadCategories(request);
            forward(request, response, EDIT_VIEW);
        } catch (EntityNotFoundException exception) {
            setFlash(request, "errorMessage", "Product not found.");
            redirectToList(request, response);
        } catch (RuntimeException exception) {
            getServletContext().log("Unable to load the product for editing.", exception);
            setFlash(request, "errorMessage", "Unable to load the product.");
            redirectToList(request, response);
        }
    }

    private void updateProduct(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer productId = parsePositiveId(request.getParameter("productId"));
        if (productId == null) {
            setFlash(request, "errorMessage", "Invalid product ID.");
            redirectToList(request, response);
            return;
        }

        Product product;
        try {
            product = productService.findById(productId);
        } catch (RuntimeException exception) {
            getServletContext().log("Unable to load the product before updating.", exception);
            setFlash(request, "errorMessage", "Unable to load the product.");
            redirectToList(request, response);
            return;
        }
        if (product == null) {
            setFlash(request, "errorMessage", "Product not found.");
            redirectToList(request, response);
            return;
        }

        String oldImage = product.getImages();
        String rawImageUrl = trimToEmpty(request.getParameter("imageUrl"));
        request.setAttribute("product", product);
        request.setAttribute("imageUrl", rawImageUrl);
        request.setAttribute("categoryId", trimToEmpty(request.getParameter("categoryId")));

        try {
            populateProduct(request, product);
        } catch (IllegalArgumentException exception) {
            product.setImages(oldImage);
            showFormError(request, response, EDIT_VIEW, exception.getMessage());
            return;
        }

        Part imagePart;
        try {
            imagePart = request.getPart("imagesFile");
        } catch (IllegalStateException exception) {
            product.setImages(oldImage);
            showFormError(request, response, EDIT_VIEW,
                    "The uploaded image must not exceed 5 MB.");
            return;
        } catch (IOException | ServletException exception) {
            getServletContext().log("Unable to read the product image upload.", exception);
            product.setImages(oldImage);
            showFormError(request, response, EDIT_VIEW, "Unable to read the selected image.");
            return;
        }

        String newLocalImage = null;
        try {
            String replacementImage = oldImage;
            if (ImageStorage.hasUpload(imagePart)) {
                newLocalImage = imageStorage.save(imagePart);
                replacementImage = newLocalImage;
            } else if (!rawImageUrl.isBlank()) {
                replacementImage = validateRemoteImageUrl(rawImageUrl);
            }
            if (replacementImage == null || replacementImage.isBlank()) {
                replacementImage = Constants.DEFAULT_IMAGE;
            }

            product.setImages(replacementImage);
            productService.update(product);
            if (!Objects.equals(oldImage, replacementImage)) {
                deleteQuietly(oldImage);
            }
            setFlash(request, "successMessage", "Product updated successfully.");
            redirectToList(request, response);
        } catch (IllegalArgumentException exception) {
            deleteQuietly(newLocalImage);
            product.setImages(oldImage);
            showFormError(request, response, EDIT_VIEW, exception.getMessage());
        } catch (RuntimeException | IOException exception) {
            deleteQuietly(newLocalImage);
            product.setImages(oldImage);
            getServletContext().log("Unable to update the product.", exception);
            showFormError(request, response, EDIT_VIEW,
                    "Unable to update the product. Please try again.");
        }
    }

    private void deleteProduct(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Integer productId = parsePositiveId(request.getParameter("id"));
        if (productId == null) {
            setFlash(request, "errorMessage", "Invalid product ID.");
            redirectToList(request, response);
            return;
        }

        try {
            Product product = productService.findById(productId);
            if (product == null) {
                setFlash(request, "errorMessage", "Product not found.");
            } else {
                String oldImage = product.getImages();
                productService.delete(productId);
                deleteQuietly(oldImage);
                setFlash(request, "successMessage", "Product deleted successfully.");
            }
        } catch (EntityNotFoundException exception) {
            setFlash(request, "errorMessage", "Product not found.");
        } catch (Exception exception) {
            getServletContext().log("Unable to delete the product.", exception);
            setFlash(request, "errorMessage", "Unable to delete the product. Please try again.");
        }
        redirectToList(request, response);
    }

    private void populateProduct(HttpServletRequest request, Product product) {
        String productName = trimToEmpty(request.getParameter("productName"));
        if (productName.isBlank()) {
            throw new IllegalArgumentException("Product name is required.");
        }
        if (productName.length() > 255) {
            throw new IllegalArgumentException("Product name must not exceed 255 characters.");
        }

        BigDecimal price = parsePrice(request.getParameter("price"));
        String description = trimToEmpty(request.getParameter("description"));
        int status = parseStatus(request.getParameter("status"));
        Integer categoryId = parsePositiveId(request.getParameter("categoryId"));
        if (categoryId == null) {
            throw new IllegalArgumentException("Please select a category.");
        }

        Category category = categoryService.findById(categoryId);
        if (category == null) {
            throw new IllegalArgumentException("The selected category does not exist.");
        }

        product.setProductName(productName);
        product.setPrice(price);
        product.setDescription(description);
        product.setStatus(status);
        product.setCategory(category);
        request.setAttribute("categoryId", categoryId);
    }

    private BigDecimal parsePrice(String rawPrice) {
        if (rawPrice == null || rawPrice.isBlank()) {
            throw new IllegalArgumentException("Price is required.");
        }
        try {
            BigDecimal price = new BigDecimal(rawPrice.trim());
            if (price.signum() < 0) {
                throw new IllegalArgumentException("Price must be zero or greater.");
            }
            return price;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Price is invalid.");
        }
    }

    private int parseStatus(String rawStatus) {
        if (!"0".equals(rawStatus) && !"1".equals(rawStatus)) {
            throw new IllegalArgumentException("Product status is invalid.");
        }
        return Integer.parseInt(rawStatus);
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

    private String validateRemoteImageUrl(String rawUrl) {
        if (rawUrl.length() > 500) {
            throw new IllegalArgumentException("Image URL must not exceed 500 characters.");
        }
        try {
            URI uri = new URI(rawUrl);
            String scheme = uri.getScheme();
            if (scheme == null
                    || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException("Image URL must be a valid HTTP or HTTPS address.");
            }
            return uri.toASCIIString();
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Image URL is invalid.");
        }
    }

    private void loadCategories(HttpServletRequest request) {
        try {
            request.setAttribute("categories", categoryService.findAll());
        } catch (RuntimeException exception) {
            getServletContext().log("Unable to load categories for the product form.", exception);
            request.setAttribute("categories", Collections.emptyList());
            if (request.getAttribute("errorMessage") == null) {
                request.setAttribute("errorMessage", "Unable to load categories.");
            }
        }
    }

    private void showFormError(HttpServletRequest request, HttpServletResponse response,
                               String view, String message)
            throws ServletException, IOException {
        request.setAttribute("errorMessage", message);
        loadCategories(request);
        forward(request, response, view);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private void deleteQuietly(String imageName) {
        try {
            imageStorage.deleteIfLocal(imageName);
        } catch (IOException exception) {
            getServletContext().log("Unable to delete local image: " + imageName, exception);
        }
    }

    private void prepareEncoding(HttpServletRequest request, HttpServletResponse response)
            throws java.io.UnsupportedEncodingException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
    }

    private void forward(HttpServletRequest request, HttpServletResponse response, String view)
            throws ServletException, IOException {
        RequestDispatcher dispatcher = request.getRequestDispatcher(view);
        dispatcher.forward(request, response);
    }

    private void redirectToList(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/admin/products");
    }

    private void setFlash(HttpServletRequest request, String key, String message) {
        request.getSession().setAttribute(key, message);
    }

    private void consumeFlashMessages(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        moveFlashToRequest(session, request, "successMessage");
        moveFlashToRequest(session, request, "errorMessage");
    }

    private void moveFlashToRequest(HttpSession session, HttpServletRequest request, String key) {
        Object message = session.getAttribute(key);
        if (message != null) {
            request.setAttribute(key, message);
            session.removeAttribute(key);
        }
    }
}
