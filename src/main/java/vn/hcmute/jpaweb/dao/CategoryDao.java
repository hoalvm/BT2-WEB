package vn.hcmute.jpaweb.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import vn.hcmute.jpaweb.config.JPAConfig;
import vn.hcmute.jpaweb.entity.Category;

import java.util.List;
import java.util.Locale;

public class CategoryDao implements ICategoryDao {

    private static final String ORDER_BY_ID = " ORDER BY c.categoryId ASC";

    @Override
    public void insert(Category category) {
        EntityManager entityManager = JPAConfig.getEntityManager();
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            entityManager.persist(category);
            transaction.commit();
        } catch (RuntimeException exception) {
            rollbackIfActive(transaction);
            throw exception;
        } finally {
            close(entityManager);
        }
    }

    @Override
    public void update(Category category) {
        EntityManager entityManager = JPAConfig.getEntityManager();
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            entityManager.merge(category);
            transaction.commit();
        } catch (RuntimeException exception) {
            rollbackIfActive(transaction);
            throw exception;
        } finally {
            close(entityManager);
        }
    }

    @Override
    public void delete(int categoryId) throws Exception {
        EntityManager entityManager = JPAConfig.getEntityManager();
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            Category category = entityManager.find(Category.class, categoryId);
            if (category == null) {
                throw new EntityNotFoundException("Không tìm thấy danh mục có mã " + categoryId);
            }
            entityManager.remove(category);
            transaction.commit();
        } catch (Exception exception) {
            rollbackIfActive(transaction);
            throw exception;
        } finally {
            close(entityManager);
        }
    }

    @Override
    public Category findById(int categoryId) {
        EntityManager entityManager = JPAConfig.getEntityManager();
        try {
            return entityManager.find(Category.class, categoryId);
        } finally {
            close(entityManager);
        }
    }

    @Override
    public Category findByCategoryName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        EntityManager entityManager = JPAConfig.getEntityManager();
        try {
            String jpql = "SELECT c FROM Category c "
                    + "WHERE LOWER(c.categoryName) = :name" + ORDER_BY_ID;
            List<Category> results = entityManager.createQuery(jpql, Category.class)
                    .setParameter("name", name.trim().toLowerCase(Locale.ROOT))
                    .setMaxResults(1)
                    .getResultList();
            return results.isEmpty() ? null : results.get(0);
        } finally {
            close(entityManager);
        }
    }

    @Override
    public List<Category> findAll() {
        EntityManager entityManager = JPAConfig.getEntityManager();
        try {
            return entityManager
                    .createNamedQuery("Category.findAll", Category.class)
                    .getResultList();
        } finally {
            close(entityManager);
        }
    }

    @Override
    public List<Category> searchByName(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }

        EntityManager entityManager = JPAConfig.getEntityManager();
        try {
            String jpql = "SELECT c FROM Category c "
                    + "WHERE LOWER(c.categoryName) LIKE :keyword" + ORDER_BY_ID;
            return entityManager.createQuery(jpql, Category.class)
                    .setParameter("keyword", toLikePattern(keyword))
                    .getResultList();
        } finally {
            close(entityManager);
        }
    }

    @Override
    public List<Category> findAll(int page, int pageSize) {
        int offset = calculateOffset(page, pageSize);
        EntityManager entityManager = JPAConfig.getEntityManager();
        try {
            TypedQuery<Category> query = entityManager
                    .createNamedQuery("Category.findAll", Category.class);
            query.setFirstResult(offset);
            query.setMaxResults(pageSize);
            return query.getResultList();
        } finally {
            close(entityManager);
        }
    }

    @Override
    public List<Category> searchByName(String keyword, int page, int pageSize) {
        if (keyword == null || keyword.isBlank()) {
            return findAll(page, pageSize);
        }

        int offset = calculateOffset(page, pageSize);
        EntityManager entityManager = JPAConfig.getEntityManager();
        try {
            String jpql = "SELECT c FROM Category c "
                    + "WHERE LOWER(c.categoryName) LIKE :keyword" + ORDER_BY_ID;
            TypedQuery<Category> query = entityManager.createQuery(jpql, Category.class);
            query.setParameter("keyword", toLikePattern(keyword));
            query.setFirstResult(offset);
            query.setMaxResults(pageSize);
            return query.getResultList();
        } finally {
            close(entityManager);
        }
    }

    @Override
    public int count() {
        EntityManager entityManager = JPAConfig.getEntityManager();
        try {
            Long result = entityManager
                    .createQuery("SELECT COUNT(c) FROM Category c", Long.class)
                    .getSingleResult();
            return Math.toIntExact(result);
        } finally {
            close(entityManager);
        }
    }

    @Override
    public int countByName(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return count();
        }

        EntityManager entityManager = JPAConfig.getEntityManager();
        try {
            String jpql = "SELECT COUNT(c) FROM Category c "
                    + "WHERE LOWER(c.categoryName) LIKE :keyword";
            Long result = entityManager.createQuery(jpql, Long.class)
                    .setParameter("keyword", toLikePattern(keyword))
                    .getSingleResult();
            return Math.toIntExact(result);
        } finally {
            close(entityManager);
        }
    }

    private static int calculateOffset(int page, int pageSize) {
        if (page < 1) {
            throw new IllegalArgumentException("Số trang phải lớn hơn hoặc bằng 1");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("Kích thước trang phải lớn hơn 0");
        }
        return Math.multiplyExact(page - 1, pageSize);
    }

    private static String toLikePattern(String keyword) {
        return "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
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
