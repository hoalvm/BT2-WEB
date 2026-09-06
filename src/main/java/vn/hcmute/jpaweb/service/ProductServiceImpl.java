package vn.hcmute.jpaweb.service;

import jakarta.persistence.EntityNotFoundException;
import vn.hcmute.jpaweb.dao.CategoryDao;
import vn.hcmute.jpaweb.dao.ICategoryDao;
import vn.hcmute.jpaweb.dao.IProductDao;
import vn.hcmute.jpaweb.dao.ProductDao;
import vn.hcmute.jpaweb.entity.Category;
import vn.hcmute.jpaweb.entity.Product;
import vn.hcmute.jpaweb.utils.Constants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

public class ProductServiceImpl implements IProductService {

    private static final int MAX_PRODUCT_NAME_LENGTH = 255;
    private static final int MAX_IMAGE_LENGTH = 500;
    private static final BigDecimal MAX_PRICE = new BigDecimal("9999999999999.99");

    private final IProductDao productDao;
    private final ICategoryDao categoryDao;

    public ProductServiceImpl() {
        this(new ProductDao(), new CategoryDao());
    }

    public ProductServiceImpl(IProductDao productDao, ICategoryDao categoryDao) {
        this.productDao = Objects.requireNonNull(productDao, "Product DAO must not be null");
        this.categoryDao = Objects.requireNonNull(categoryDao, "Category DAO must not be null");
    }

    @Override
    public void insert(Product product) {
        validateAndNormalize(product);
        product.setCategory(requireCategory(product));
        productDao.insert(product);
    }

    @Override
    public void update(Product product) {
        validateAndNormalize(product);
        Integer productId = product.getProductId();
        if (productId == null || productId < 1) {
            throw new IllegalArgumentException("Product ID is not valid");
        }
        if (productDao.findById(productId) == null) {
            throw new EntityNotFoundException("Product not found: " + productId);
        }
        product.setCategory(requireCategory(product));
        productDao.update(product);
    }

    @Override
    public void delete(int productId) throws Exception {
        validateProductId(productId);
        if (productDao.findById(productId) == null) {
            throw new EntityNotFoundException("Product not found: " + productId);
        }
        productDao.delete(productId);
    }

    @Override
    public Product findById(int productId) {
        validateProductId(productId);
        return productDao.findById(productId);
    }

    @Override
    public List<Product> findAll() {
        return productDao.findAll();
    }

    @Override
    public Product findActiveById(int productId) {
        validateProductId(productId);
        return productDao.findActiveById(productId);
    }

    @Override
    public List<Product> findActivePage(int page, int pageSize) {
        validatePage(page, pageSize);
        return productDao.findActivePage(page, pageSize);
    }

    @Override
    public int countActive() {
        return productDao.countActive();
    }

    @Override
    public List<Product> findNewestActive(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be greater than 0");
        }
        return productDao.findNewestActive(limit);
    }

    private Category requireCategory(Product product) {
        if (product.getCategory() == null || product.getCategory().getCategoryId() == null
                || product.getCategory().getCategoryId() < 1) {
            throw new IllegalArgumentException("Product category is required");
        }
        Category category = categoryDao.findById(product.getCategory().getCategoryId());
        if (category == null) {
            throw new EntityNotFoundException(
                    "Category not found: " + product.getCategory().getCategoryId()
            );
        }
        return category;
    }

    private static void validateAndNormalize(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product must not be null");
        }

        String productName = product.getProductName();
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        productName = productName.trim();
        if (productName.length() > MAX_PRODUCT_NAME_LENGTH) {
            throw new IllegalArgumentException("Product name must not exceed 255 characters");
        }
        product.setProductName(productName);

        BigDecimal price = product.getPrice();
        if (price == null) {
            throw new IllegalArgumentException("Product price is required");
        }
        if (price.signum() < 0 || price.compareTo(MAX_PRICE) > 0) {
            throw new IllegalArgumentException("Product price must be between 0 and " + MAX_PRICE);
        }
        try {
            product.setPrice(price.setScale(2, RoundingMode.UNNECESSARY));
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Product price must have at most 2 decimal places");
        }

        String description = product.getDescription();
        product.setDescription(description == null || description.isBlank() ? null : description.trim());

        String images = product.getImages();
        if (images == null || images.isBlank()) {
            product.setImages(Constants.DEFAULT_IMAGE);
        } else {
            images = images.trim();
            if (images.length() > MAX_IMAGE_LENGTH) {
                throw new IllegalArgumentException("Product image path must not exceed 500 characters");
            }
            product.setImages(images);
        }

        if (product.getStatus() != 0 && product.getStatus() != 1) {
            throw new IllegalArgumentException("Product status must be 0 or 1");
        }
    }

    private static void validateProductId(int productId) {
        if (productId < 1) {
            throw new IllegalArgumentException("Product ID must be greater than 0");
        }
    }

    private static void validatePage(int page, int pageSize) {
        if (page < 1) {
            throw new IllegalArgumentException("Page must be greater than or equal to 1");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("Page size must be greater than 0");
        }
    }
}
