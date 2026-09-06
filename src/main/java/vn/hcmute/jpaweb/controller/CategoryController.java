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
import vn.hcmute.jpaweb.service.CategoryServiceImpl;
import vn.hcmute.jpaweb.service.ICategoryService;
import vn.hcmute.jpaweb.utils.Constants;
import vn.hcmute.jpaweb.utils.ImageStorage;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@WebServlet(
        name = "CategoryController",
        urlPatterns = {
                "/admin/categories",
                "/admin/category/add",
                "/admin/category/insert",
                "/admin/category/edit",
                "/admin/category/update",
                "/admin/category/delete"
        }
)
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = Constants.MAX_UPLOAD_BYTES,
        maxRequestSize = Constants.MAX_UPLOAD_BYTES + 1024 * 1024
)
public class CategoryController extends HttpServlet {

    private static final String LIST_VIEW = "/views/admin/category/category-list.jsp";
    private static final String ADD_VIEW = "/views/admin/category/category-add.jsp";
    private static final String EDIT_VIEW = "/views/admin/category/category-edit.jsp";

    private transient ICategoryService categoryService;
    private transient ImageStorage imageStorage;

    @Override
    public void init() throws ServletException {
        categoryService = new CategoryServiceImpl();
        imageStorage = new ImageStorage();
        try {
            imageStorage.ensureUploadDirectory();
            getServletContext().log("JPA Web upload directory: " + Constants.UPLOAD_DIRECTORY);
        } catch (IOException exception) {
            throw new UnavailableException("Không thể khởi tạo thư mục upload ảnh.");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        prepareEncoding(request, response);

        switch (request.getServletPath()) {
            case "/admin/categories" -> showCategoryList(request, response);
            case "/admin/category/add" -> showAddForm(request, response);
            case "/admin/category/edit" -> showEditForm(request, response);
            case "/admin/category/delete" -> deleteCategory(request, response);
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
                case "/admin/category/insert" -> insertCategory(request, response);
                case "/admin/category/update" -> updateCategory(request, response);
                default -> response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
        } catch (IllegalStateException exception) {
            // Tomcat có thể parse multipart ngay từ lần getParameter() đầu tiên và
            // ném lỗi trước khi controller gọi getPart(). Vì vậy cần chặn ở biên
            // doPost để request quá dung lượng không biến thành HTTP 500.
            setFlash(request, "errorMessage", "Ảnh tải lên không được vượt quá 5 MB.");
            if ("/admin/category/insert".equals(servletPath)) {
                response.sendRedirect(request.getContextPath() + "/admin/category/add");
            } else {
                redirectToList(request, response);
            }
        }
    }

    private void showCategoryList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlashMessages(request);

        String keyword = trimToEmpty(request.getParameter("keyword"));
        if (keyword.length() > 255) {
            keyword = keyword.substring(0, 255);
        }
        int requestedPage = parsePage(request.getParameter("page"));
        int pageSize = Constants.CATEGORY_PAGE_SIZE;

        try {
            int totalItems = keyword.isBlank()
                    ? categoryService.count()
                    : categoryService.countByName(keyword);
            int totalPages = totalItems == 0 ? 0 : (totalItems + pageSize - 1) / pageSize;
            int currentPage = totalPages == 0 ? 1 : Math.min(requestedPage, totalPages);
            List<Category> categories = keyword.isBlank()
                    ? categoryService.findAll(currentPage, pageSize)
                    : categoryService.searchByName(keyword, currentPage, pageSize);

            request.setAttribute("categories", categories);
            request.setAttribute("currentPage", currentPage);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("totalItems", totalItems);
        } catch (RuntimeException exception) {
            getServletContext().log("Không thể tải danh sách danh mục.", exception);
            request.setAttribute("categories", Collections.emptyList());
            request.setAttribute("currentPage", 1);
            request.setAttribute("totalPages", 0);
            request.setAttribute("totalItems", 0);
            if (request.getAttribute("errorMessage") == null) {
                request.setAttribute("errorMessage",
                        "Không thể tải danh sách danh mục. Vui lòng kiểm tra kết nối cơ sở dữ liệu.");
            }
        }

        request.setAttribute("keyword", keyword);
        request.setAttribute("pageSize", pageSize);
        forward(request, response, LIST_VIEW);
    }

    private void showAddForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlashMessages(request);
        Category category = new Category();
        category.setStatus(1);
        request.setAttribute("category", category);
        forward(request, response, ADD_VIEW);
    }

    private void insertCategory(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String categoryName = trimToEmpty(request.getParameter("categoryName"));
        String rawImageUrl = trimToEmpty(request.getParameter("imageUrl"));
        Category category = new Category();
        category.setCategoryName(categoryName);
        request.setAttribute("category", category);
        request.setAttribute("imageUrl", rawImageUrl);

        try {
            category.setStatus(parseStatus(request.getParameter("status")));
            validateCategoryName(categoryName);
        } catch (IllegalArgumentException exception) {
            request.setAttribute("errorMessage", exception.getMessage());
            forward(request, response, ADD_VIEW);
            return;
        }

        Part imagePart;
        try {
            imagePart = request.getPart("imagesFile");
        } catch (IllegalStateException exception) {
            request.setAttribute("errorMessage", "Ảnh tải lên không được vượt quá 5 MB.");
            forward(request, response, ADD_VIEW);
            return;
        } catch (IOException | ServletException exception) {
            getServletContext().log("Không thể đọc dữ liệu upload.", exception);
            request.setAttribute("errorMessage", "Không thể đọc file ảnh đã chọn.");
            forward(request, response, ADD_VIEW);
            return;
        }

        String newLocalImage = null;
        try {
            if (ImageStorage.hasUpload(imagePart)) {
                newLocalImage = imageStorage.save(imagePart);
                category.setImages(newLocalImage);
            } else if (!rawImageUrl.isBlank()) {
                category.setImages(validateRemoteImageUrl(rawImageUrl));
            } else {
                category.setImages(Constants.DEFAULT_IMAGE);
            }

            categoryService.insert(category);
            setFlash(request, "successMessage", "Thêm danh mục thành công.");
            redirectToList(request, response);
        } catch (IllegalArgumentException exception) {
            deleteQuietly(newLocalImage);
            category.setImages(Constants.DEFAULT_IMAGE);
            request.setAttribute("errorMessage", exception.getMessage());
            forward(request, response, ADD_VIEW);
        } catch (RuntimeException | IOException exception) {
            deleteQuietly(newLocalImage);
            getServletContext().log("Không thể thêm danh mục.", exception);
            category.setImages(Constants.DEFAULT_IMAGE);
            request.setAttribute("errorMessage", "Không thể thêm danh mục. Vui lòng thử lại.");
            forward(request, response, ADD_VIEW);
        }
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer categoryId = parsePositiveId(request.getParameter("id"));
        if (categoryId == null) {
            setFlash(request, "errorMessage", "Mã danh mục không hợp lệ.");
            redirectToList(request, response);
            return;
        }

        try {
            Category category = categoryService.findById(categoryId);
            if (category == null) {
                setFlash(request, "errorMessage", "Không tìm thấy danh mục cần sửa.");
                redirectToList(request, response);
                return;
            }
            consumeFlashMessages(request);
            request.setAttribute("category", category);
            if (ImageStorage.isRemoteImage(category.getImages())) {
                request.setAttribute("imageUrl", category.getImages());
            }
            forward(request, response, EDIT_VIEW);
        } catch (EntityNotFoundException exception) {
            setFlash(request, "errorMessage", "Không tìm thấy danh mục cần sửa.");
            redirectToList(request, response);
        } catch (RuntimeException exception) {
            getServletContext().log("Không thể tải danh mục cần sửa.", exception);
            setFlash(request, "errorMessage", "Không thể tải thông tin danh mục.");
            redirectToList(request, response);
        }
    }

    private void updateCategory(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer categoryId = parsePositiveId(request.getParameter("categoryId"));
        if (categoryId == null) {
            setFlash(request, "errorMessage", "Mã danh mục không hợp lệ.");
            redirectToList(request, response);
            return;
        }

        Category category;
        try {
            category = categoryService.findById(categoryId);
        } catch (EntityNotFoundException exception) {
            category = null;
        } catch (RuntimeException exception) {
            getServletContext().log("Không thể tải danh mục trước khi cập nhật.", exception);
            setFlash(request, "errorMessage", "Không thể tải thông tin danh mục.");
            redirectToList(request, response);
            return;
        }
        if (category == null) {
            setFlash(request, "errorMessage", "Không tìm thấy danh mục cần cập nhật.");
            redirectToList(request, response);
            return;
        }

        String oldImage = category.getImages();
        String categoryName = trimToEmpty(request.getParameter("categoryName"));
        String rawImageUrl = trimToEmpty(request.getParameter("imageUrl"));
        category.setCategoryName(categoryName);
        request.setAttribute("category", category);
        request.setAttribute("imageUrl", rawImageUrl);

        try {
            category.setStatus(parseStatus(request.getParameter("status")));
            validateCategoryName(categoryName);
        } catch (IllegalArgumentException exception) {
            request.setAttribute("errorMessage", exception.getMessage());
            forward(request, response, EDIT_VIEW);
            return;
        }

        Part imagePart;
        try {
            imagePart = request.getPart("imagesFile");
        } catch (IllegalStateException exception) {
            request.setAttribute("errorMessage", "Ảnh tải lên không được vượt quá 5 MB.");
            forward(request, response, EDIT_VIEW);
            return;
        } catch (IOException | ServletException exception) {
            getServletContext().log("Không thể đọc dữ liệu upload.", exception);
            request.setAttribute("errorMessage", "Không thể đọc file ảnh đã chọn.");
            forward(request, response, EDIT_VIEW);
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

            category.setImages(replacementImage);
            categoryService.update(category);

            if (!Objects.equals(oldImage, replacementImage)) {
                deleteQuietly(oldImage);
            }
            setFlash(request, "successMessage", "Cập nhật danh mục thành công.");
            redirectToList(request, response);
        } catch (IllegalArgumentException exception) {
            deleteQuietly(newLocalImage);
            category.setImages(oldImage);
            request.setAttribute("errorMessage", exception.getMessage());
            forward(request, response, EDIT_VIEW);
        } catch (RuntimeException | IOException exception) {
            deleteQuietly(newLocalImage);
            category.setImages(oldImage);
            getServletContext().log("Không thể cập nhật danh mục.", exception);
            request.setAttribute("errorMessage", "Không thể cập nhật danh mục. Vui lòng thử lại.");
            forward(request, response, EDIT_VIEW);
        }
    }

    private void deleteCategory(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Integer categoryId = parsePositiveId(request.getParameter("id"));
        if (categoryId == null) {
            setFlash(request, "errorMessage", "Mã danh mục không hợp lệ.");
            redirectToList(request, response);
            return;
        }

        try {
            Category category = categoryService.findById(categoryId);
            if (category == null) {
                setFlash(request, "errorMessage", "Không tìm thấy danh mục cần xóa.");
            } else {
                String oldImage = category.getImages();
                categoryService.delete(categoryId);
                deleteQuietly(oldImage);
                setFlash(request, "successMessage", "Xóa danh mục thành công.");
            }
        } catch (EntityNotFoundException exception) {
            setFlash(request, "errorMessage", "Không tìm thấy danh mục cần xóa.");
        } catch (IllegalArgumentException exception) {
            setFlash(request, "errorMessage", exception.getMessage());
        } catch (Exception exception) {
            getServletContext().log("Không thể xóa danh mục.", exception);
            setFlash(request, "errorMessage",
                    "Không thể xóa danh mục. Danh mục đang được video hoặc sản phẩm sử dụng.");
        }
        redirectToList(request, response);
    }

    private String validateRemoteImageUrl(String rawUrl) {
        if (rawUrl.length() > 500) {
            throw new IllegalArgumentException("URL ảnh không được vượt quá 500 ký tự.");
        }
        try {
            URI uri = new URI(rawUrl);
            String scheme = uri.getScheme();
            if (scheme == null
                    || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException("URL ảnh phải là địa chỉ HTTP hoặc HTTPS hợp lệ.");
            }
            return uri.toASCIIString();
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("URL ảnh không hợp lệ.");
        }
    }

    private void validateCategoryName(String categoryName) {
        if (categoryName.isBlank()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống.");
        }
        if (categoryName.length() > 255) {
            throw new IllegalArgumentException("Tên danh mục không được vượt quá 255 ký tự.");
        }
    }

    private int parseStatus(String rawStatus) {
        if (!"0".equals(rawStatus) && !"1".equals(rawStatus)) {
            throw new IllegalArgumentException("Trạng thái danh mục không hợp lệ.");
        }
        return Integer.parseInt(rawStatus);
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

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private void deleteQuietly(String imageName) {
        try {
            imageStorage.deleteIfLocal(imageName);
        } catch (IOException exception) {
            getServletContext().log("Không thể xóa ảnh local: " + imageName, exception);
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
        response.sendRedirect(request.getContextPath() + "/admin/categories");
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
