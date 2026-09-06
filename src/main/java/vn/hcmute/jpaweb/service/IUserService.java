package vn.hcmute.jpaweb.service;

import vn.hcmute.jpaweb.entity.OtpPurpose;
import vn.hcmute.jpaweb.entity.User;

public interface IUserService {

    User register(String username, String email, String password, String confirmPassword);

    User authenticate(String identifier, String password);

    User requestPasswordReset(String email);

    void verifyOtp(int userId, OtpPurpose purpose, String code);

    void resendOtp(int userId, OtpPurpose purpose);

    void resetPassword(int userId, String password, String confirmPassword);

    User findById(int userId);
}
