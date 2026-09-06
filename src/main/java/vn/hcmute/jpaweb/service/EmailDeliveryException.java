package vn.hcmute.jpaweb.service;

import vn.hcmute.jpaweb.entity.OtpPurpose;

public class EmailDeliveryException extends IllegalStateException {

    private final int userId;
    private final OtpPurpose purpose;

    public EmailDeliveryException(int userId, OtpPurpose purpose, String message) {
        super(message);
        this.userId = userId;
        this.purpose = purpose;
    }

    public EmailDeliveryException(int userId, OtpPurpose purpose, String message, Throwable cause) {
        super(message, cause);
        this.userId = userId;
        this.purpose = purpose;
    }

    public int getUserId() {
        return userId;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }
}
