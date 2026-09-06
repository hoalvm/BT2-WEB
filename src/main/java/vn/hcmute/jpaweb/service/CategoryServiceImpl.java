package vn.hcmute.jpaweb.service;

import jakarta.persistence.EntityNotFoundException;
import vn.hcmute.jpaweb.dao.CategoryDao;
import vn.hcmute.jpaweb.dao.ICategoryDao;
import vn.hcmute.jpaweb.entity.Category;

import java.util.List;

public class CategoryServiceImpl implements ICategoryService {

    private static final int MAX_CATEGORY_NAME_LENGTH = 255;
    private static final int MAX_IMAGE_LENGTH = 500;
    private static final String DEFAULT_IMAGE = "default.png";

    private final ICategoryDao categoryDao;

    public CategoryServiceImpl() {
        this(new CategoryDao());
    }

    public CategoryServiceImpl(ICategoryDao categoryDao) {
        if (categoryDao == null) {
            throw new IllegalArgumentException("Category DAO không được null");
        }
        this.categoryDao = categoryDao;
    }

    @Override
    public void insert(Category category) {
        validateAndNormalize(category);

        Category duplicate = categoryDao.findByCategoryName(category.getCategoryName());
        if (duplicate != null) {
            throw new IllegalArgumentException("Tên danh mục đã tồn tại");
        }

        categoryDao.insert(category);
    }

    @Override
    public void update(Category category) {
        validateAndNormalize(category);
        Integer categoryId = category.getCategoryId();
        if (categoryId == null || categoryId < 1) {
            throw new IllegalArgumentException("Mã danh mục không hợp lệ");
        }

        Category existing = categoryDao.findById(categoryId);
        if (existing == null) {
            throw new EntityNotFoundException("Không tìm thấy danh mục có mã " + categoryId);
        }

        Category duplicate = categoryDao.findByCategoryName(category.getCategoryName());
        if (duplicate != null && !duplicate.getCategoryId().equals(categoryId)) {
            throw new IllegalArgumentException("Tên danh mục đã tồn tại");
        }

        existing.setCategoryName(category.getCategoryName());
        existing.setImages(category.getImages());
        existing.setStatus(category.getStatus());
        categoryDao.update(existing);
    }

    @Override
    public void delete(int categoryId) throws Exception {
        validateCategoryId(categoryId);
        if (categoryDao.findById(categoryId) == null) {
            throw new EntityNotFoundException("Không tìm thấy danh mục có mã " + categoryId);
        }
        categoryDao.delete(categoryId);
    }

    @Override
    public Category findById(int categoryId) {
        validateCategoryId(categoryId);
        return categoryDao.findById(categoryId);
    }

    @Override
    public Category findByCategoryName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return categoryDao.findByCategoryName(name.trim());
    }

    @Override
    public List<Category> findAll() {
        return categoryDao.findAll();
    }

    @Override
    public List<Category> searchByName(String keyword) {
        return categoryDao.searchByName(normalizeKeyword(keyword));
    }

    @Override
    public List<Category> findAll(int page, int pageSize) {
        validatePage(page, pageSize);
        return categoryDao.findAll(page, pageSize);
    }

    @Override
    public List<Category> searchByName(String keyword, int page, int pageSize) {
        validatePage(page, pageSize);
        return categoryDao.searchByName(normalizeKeyword(keyword), page, pageSize);
    }

    @Override
    public int count() {
        return categoryDao.count();
    }

    @Override
    public int countByName(String keyword) {
        return categoryDao.countByName(normalizeKeyword(keyword));
    }

    private static void validateAndNormalize(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Danh mục không được null");
        }

        String categoryName = category.getCategoryName();
        if (categoryName == null || categoryName.isBlank()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống");
        }
        categoryName = categoryName.trim();
        if (categoryName.length() > MAX_CATEGORY_NAME_LENGTH) {
            throw new IllegalArgumentException("Tên danh mục không được vượt quá 255 ký tự");
        }
        category.setCategoryName(categoryName);

        String images = category.getImages();
        if (images == null || images.isBlank()) {
            category.setImages(DEFAULT_IMAGE);
        } else {
            images = images.trim();
            if (images.length() > MAX_IMAGE_LENGTH) {
                throw new IllegalArgumentException("Đường dẫn ảnh không được vượt quá 500 ký tự");
            }
            category.setImages(images);
        }

        if (category.getStatus() != 0 && category.getStatus() != 1) {
            throw new IllegalArgumentException("Trạng thái chỉ nhận giá trị 0 hoặc 1");
        }
    }

    private static void validateCategoryId(int categoryId) {
        if (categoryId < 1) {
            throw new IllegalArgumentException("Mã danh mục phải lớn hơn 0");
        }
    }

    private static void validatePage(int page, int pageSize) {
        if (page < 1) {
            throw new IllegalArgumentException("Số trang phải lớn hơn hoặc bằng 1");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("Kích thước trang phải lớn hơn 0");
        }
    }

    private static String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }
}
