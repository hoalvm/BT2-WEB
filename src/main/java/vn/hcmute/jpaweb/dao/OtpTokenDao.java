package vn.hcmute.jpaweb.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import vn.hcmute.jpaweb.config.JPAConfig;
import vn.hcmute.jpaweb.entity.OtpPurpose;
import vn.hcmute.jpaweb.entity.OtpToken;
import vn.hcmute.jpaweb.entity.User;
import vn.hcmute.jpaweb.utils.OTPUtil;

import java.time.LocalDateTime;
import java.util.List;

public class OtpTokenDao implements IOtpTokenDao {

    @Override
    public void replaceActive(OtpToken token) {
        EntityManager entityManager = JPAConfig.getEntityManager();
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();

            Integer userId = requireUserId(token);
            LocalDateTime invalidatedAt = token.getLastSentAt() == null
                    ? LocalDateTime.now()
                    : token.getLastSentAt();
            entityManager.createQuery(
                            "UPDATE OtpToken o SET o.usedAt = :usedAt "
                                    + "WHERE o.user.userId = :userId AND o.purpose = :purpose "
                                    + "AND o.usedAt IS NULL"
                    )
                    .setParameter("usedAt", invalidatedAt)
                    .setParameter("userId", userId)
                    .setParameter("purpose", token.getPurpose())
                    .executeUpdate();

            User managedUser = entityManager.find(User.class, userId);
            if (managedUser == null) {
                throw new EntityNotFoundException("User not found: " + userId);
            }
            token.setUser(managedUser);
            entityManager.persist(token);
            transaction.commit();
        } catch (RuntimeException exception) {
            rollbackIfActive(transaction);
            throw exception;
        } finally {
            close(entityManager);
        }
    }

    @Override
    public void update(OtpToken token) {
        EntityManager entityManager = JPAConfig.getEntityManager();
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            entityManager.merge(token);
            transaction.commit();
        } catch (RuntimeException exception) {
            rollbackIfActive(transaction);
            throw exception;
        } finally {
            close(entityManager);
        }
    }

    @Override
    public void activateUserAndConsume(OtpToken token, LocalDateTime completedAt) {
        EntityManager entityManager = JPAConfig.getEntityManager();
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            OtpToken managedToken = requireLockedToken(entityManager, token);
            if (managedToken.getPurpose() != OtpPurpose.REGISTER) {
                throw new IllegalStateException("OTP purpose is not valid for account activation");
            }
            if (managedToken.isUsed()) {
                throw new IllegalStateException("OTP has already been used");
            }

            LocalDateTime completionTime = requireCompletionTime(completedAt);
            if (managedToken.isExpired(completionTime)
                    || managedToken.getAttemptsRemaining() <= 0) {
                throw new IllegalStateException("OTP is no longer valid");
            }
            managedToken.setVerifiedAt(completionTime);
            managedToken.setUsedAt(completionTime);
            managedToken.getUser().setActive(true);
            transaction.commit();

            token.setVerifiedAt(completionTime);
            token.setUsedAt(completionTime);
            if (token.getUser() != null) {
                token.getUser().setActive(true);
            }
        } catch (RuntimeException exception) {
            rollbackIfActive(transaction);
            throw exception;
        } finally {
            close(entityManager);
        }
    }

    @Override
    public void updatePasswordAndConsume(OtpToken token, String passwordHash,
                                         LocalDateTime completedAt) {
        EntityManager entityManager = JPAConfig.getEntityManager();
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            OtpToken managedToken = requireLockedToken(entityManager, token);
            if (managedToken.getPurpose() != OtpPurpose.RESET_PASSWORD
                    || managedToken.isUsed() || !managedToken.isVerified()) {
                throw new IllegalStateException("Password reset OTP is no longer valid");
            }

            LocalDateTime completionTime = requireCompletionTime(completedAt);
            if (!OTPUtil.resetAuthorizationExpiryFrom(managedToken.getVerifiedAt())
                    .isAfter(completionTime)) {
                throw new IllegalStateException("Password reset authorization has expired");
            }
            if (passwordHash == null || passwordHash.isBlank()) {
                throw new IllegalArgumentException("Password hash must not be empty");
            }

            managedToken.getUser().setPasswordHash(passwordHash);
            managedToken.setUsedAt(completionTime);
            transaction.commit();

            token.setUsedAt(completionTime);
            if (token.getUser() != null) {
                token.getUser().setPasswordHash(passwordHash);
            }
        } catch (RuntimeException exception) {
            rollbackIfActive(transaction);
            throw exception;
        } finally {
            close(entityManager);
        }
    }

    @Override
    public OtpToken findLatest(int userId, OtpPurpose purpose) {
        EntityManager entityManager = JPAConfig.getEntityManager();
        try {
            List<OtpToken> tokens = entityManager.createQuery(
                            "SELECT o FROM OtpToken o JOIN FETCH o.user "
                                    + "WHERE o.user.userId = :userId AND o.purpose = :purpose "
                                    + "ORDER BY o.otpId DESC",
                            OtpToken.class
                    )
                    .setParameter("userId", userId)
                    .setParameter("purpose", purpose)
                    .setMaxResults(1)
                    .getResultList();
            return tokens.isEmpty() ? null : tokens.get(0);
        } finally {
            close(entityManager);
        }
    }

    private static Integer requireUserId(OtpToken token) {
        if (token == null || token.getUser() == null || token.getUser().getUserId() == null) {
            throw new IllegalArgumentException("OTP token must reference a persisted user");
        }
        if (token.getPurpose() == null) {
            throw new IllegalArgumentException("OTP purpose must not be null");
        }
        return token.getUser().getUserId();
    }

    private static OtpToken requireLockedToken(EntityManager entityManager, OtpToken token) {
        if (token == null || token.getOtpId() == null) {
            throw new IllegalArgumentException("OTP token must already be persisted");
        }
        OtpToken managedToken = entityManager.find(
                OtpToken.class,
                token.getOtpId(),
                LockModeType.PESSIMISTIC_WRITE
        );
        if (managedToken == null) {
            throw new EntityNotFoundException("OTP token not found: " + token.getOtpId());
        }
        return managedToken;
    }

    private static LocalDateTime requireCompletionTime(LocalDateTime completedAt) {
        if (completedAt == null) {
            throw new IllegalArgumentException("Completion time must not be null");
        }
        return completedAt;
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
