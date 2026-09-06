package vn.hcmute.jpaweb.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import vn.hcmute.jpaweb.config.JPAConfig;
import vn.hcmute.jpaweb.entity.User;

import java.util.List;
import java.util.Locale;

public class UserDao implements IUserDao {

    @Override
    public void insert(User user) {
        EntityManager entityManager = JPAConfig.getEntityManager();
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            entityManager.persist(user);
            transaction.commit();
        } catch (RuntimeException exception) {
            rollbackIfActive(transaction);
            throw exception;
        } finally {
            close(entityManager);
        }
    }

    @Override
    public void update(User user) {
        EntityManager entityManager = JPAConfig.getEntityManager();
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            entityManager.merge(user);
            transaction.commit();
        } catch (RuntimeException exception) {
            rollbackIfActive(transaction);
            throw exception;
        } finally {
            close(entityManager);
        }
    }

    @Override
    public User findById(int userId) {
        EntityManager entityManager = JPAConfig.getEntityManager();
        try {
            return entityManager.find(User.class, userId);
        } finally {
            close(entityManager);
        }
    }

    @Override
    public User findByUsername(String username) {
        return findSingleBy("LOWER(u.username) = :value", normalize(username));
    }

    @Override
    public User findByEmail(String email) {
        return findSingleBy("LOWER(u.email) = :value", normalize(email));
    }

    @Override
    public User findByUsernameOrEmail(String identifier) {
        String value = normalize(identifier);
        if (value == null) {
            return null;
        }

        EntityManager entityManager = JPAConfig.getEntityManager();
        try {
            List<User> users = entityManager.createQuery(
                            "SELECT u FROM User u "
                                    + "WHERE LOWER(u.username) = :value OR LOWER(u.email) = :value",
                            User.class
                    )
                    .setParameter("value", value)
                    .setMaxResults(1)
                    .getResultList();
            return users.isEmpty() ? null : users.get(0);
        } finally {
            close(entityManager);
        }
    }

    private User findSingleBy(String predicate, String value) {
        if (value == null) {
            return null;
        }

        EntityManager entityManager = JPAConfig.getEntityManager();
        try {
            List<User> users = entityManager.createQuery(
                            "SELECT u FROM User u WHERE " + predicate,
                            User.class
                    )
                    .setParameter("value", value)
                    .setMaxResults(1)
                    .getResultList();
            return users.isEmpty() ? null : users.get(0);
        } finally {
            close(entityManager);
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
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
