package vn.hcmute.jpaweb.dao;

import vn.hcmute.jpaweb.entity.OtpPurpose;
import vn.hcmute.jpaweb.entity.OtpToken;

import java.time.LocalDateTime;

public interface IOtpTokenDao {

    void replaceActive(OtpToken token);

    void update(OtpToken token);

    void activateUserAndConsume(OtpToken token, LocalDateTime completedAt);

    void updatePasswordAndConsume(OtpToken token, String passwordHash, LocalDateTime completedAt);

    OtpToken findLatest(int userId, OtpPurpose purpose);
}
