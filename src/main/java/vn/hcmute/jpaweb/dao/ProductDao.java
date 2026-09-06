package vn.hcmute.jpaweb.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import vn.hcmute.jpaweb.config.JPAConfig;
import vn.hcmute.jpaweb.entity.Category;
import vn.hcmute.jpaweb.entity.Product;

import java.util.List;

public class ProductDao implements IProductDao {

    private static final String SELECT_WITH_CATEGORY =
            "SELECT p FROM Product p JOIN FETCH p.category c";
    private static final String NEWEST_ORDER =
            " ORDER BY p.createDate DESC, p.productId DESC";

    @Override
    public void insert(Product product) {
        EntityManager entityManager = JPAConfig.getEntityManager();
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            product.setCategory(requireManagedCategory(entityManager, product));
            entityManager.persist(product);
            transaction.commit();
        } catch (RuntimeException exception) {
            rollbackIfActive(transaction);
            throw exception;
        } finally {
            close(entityManager);
        }
    }

    @Override
    public void update(Product product) {
        EntityManager entityManager = JPAConfig.getEntityManager();
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();

            Product managedProduct = entityManager.find(Product.class, product.getProductId());
            if (managedProduct == null) {
                throw new EntityNotFoundException("Product not found: " + product.getProductId());
            }

            managedProduct.setProductName(product.getProductName());
            managedProduct.setPrice(product.getPrice());
            managedProduct.setDescription(product.getDescription());
            managedProduct.setImages(product.getImages());
            managedProduct.setStatus(product.getStatus());
            managedProduct.setCategory(requireManagedCategory(entityManager, product));
            transaction.commit();

            product.setCreateDate(managedProduct.getCreateDate());
            product.setCategory(managedProduct.getCategory());
        } catch (RuntimeException exception) {
            rollbackIfActive(transaction);
            throw exception;
        } finally {
            close(entityManager);
        }
    }

    @Override
    public void delete(int productId) throws Exception {
        EntityManager entityManager = JPAConfig.getEntityManager();
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            Product product = entityManager.find(Product.class, productId);
            if (product == null) {
                throw new EntityNotFoundException("Product not found: " + productId);
            }
            entityManager.remove(product);
            transaction.commit();
        } catch (Exception exception) {
            rollbackIfActive(transaction);
            throw exception;
        } finally {
            close(entityManager);
        }
    }

    @Override
    public Product findById(int productId) {
        return findSingle(SELECT_WITH_CATEGORY + " WHERE p.productId = :productId", productId);
    }

    @Override
    public List<Product> findAll() {
        EntityManager entityManager = JPAConfig.getEntityManager();
        try {
            return entityManager.createQuery(SELECT_WITH_CATEGORY + NEWEST_ORDER, Product.class)
                    .getResultList();
        } finally {
            close(entityManager);
        }
    }

    @Override
    public Product findActiveById(int productId) {
        return findSingle(
                SELECT_WITH_CATEGORY + " WHERE p.productId = :productId AND p.status = 1",
                productId
        );
    }

    @Override
    public List<Product> findActivePage(int page, int pageSize) {
        int offset = calculateOffset(page, pageSize);
        EntityManager entityManager = JPAConfig.getEntityManager();
        try {
            TypedQuery<Product> query = entityManager.createQuery(
                    SELECT_WITH_CATEGORY + " WHERE p.status = 1" + NEWEST_ORDER,
                    Product.class
            );
            query.setFirstResult(offset);
            query.setMaxResults(pageSize);
            return query.getResultList();
        } finally {
            close(entityManager);
        }
    }

    @Override
    public int countActive() {
        EntityManager entityManager = JPAConfig.getEntityManager();
        try {
            Long result = entityManager.createQuery(
                            "SELECT COUNT(p) FROM Product p WHERE p.status = 1",
                            Long.class
                    )
                    .getSingleResult();
            return Math.toIntExact(result);
        } finally {
            close(entityManager);
        }
    }

    @Override
    public List<Product> findNewestActive(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be greater than 0");
        }
        EntityManager entityManager = JPAConfig.getEntityManager();
        try {
            return entityManager.createQuery(
                            SELECT_WITH_CATEGORY + " WHERE p.status = 1" + NEWEST_ORDER,
                            Product.class
                    )
                    .setMaxResults(limit)
                    .getResultList();
        } finally {
            close(entityManager);
        }
    }

    private Product findSingle(String jpql, int productId) {
        EntityManager entityManager = JPAConfig.getEntityManager();
        try {
            List<Product> products = entityManager.createQuery(jpql, Product.class)
                    .setParameter("productId", productId)
                    .setMaxResults(1)
                    .getResultList();
            return products.isEmpty() ? null : products.get(0);
        } finally {
            close(entityManager);
        }
    }

    private static Category requireManagedCategory(EntityManager entityManager, Product product) {
        if (product == null || product.getCategory() == null
                || product.getCategory().getCategoryId() == null) {
            throw new IllegalArgumentException("Product category is required");
        }
        Integer categoryId = product.getCategory().getCategoryId();
        Category category = entityManager.find(Category.class, categoryId);
        if (category == null) {
            throw new EntityNotFoundException("Category not found: " + categoryId);
        }
        return category;
    }

    private static int calculateOffset(int page, int pageSize) {
        if (page < 1) {
            throw new IllegalArgumentException("Page must be greater than or equal to 1");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("Page size must be greater than 0");
        }
        return Math.multiplyExact(page - 1, pageSize);
    }

    private static void rollbackIfActive(EntityTransaction transaction) {
        if (transaction != null && transaction.isActive()) {
            transaction.rollback();
        }
    }

    private static void close(EntityManager entityManager) {
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
    }
}
