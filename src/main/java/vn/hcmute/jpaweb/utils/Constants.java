package vn.hcmute.jpaweb.utils;

import java.nio.file.Path;
import java.util.Set;

/**
 * Các hằng số dùng chung cho tầng web.
 */
public final class Constants {

    public static final String UPLOAD_DIRECTORY_PROPERTY = "jpaweb.upload.dir";
    public static final String UPLOAD_DIRECTORY_ENV = "JPAWEB_UPLOAD_DIR";
    public static final String DEFAULT_IMAGE = "default.png";
    public static final int CATEGORY_PAGE_SIZE = 5;
    public static final int PRODUCT_PAGE_SIZE = 6;
    public static final int HOME_PRODUCT_LIMIT = 10;
    public static final long MAX_UPLOAD_BYTES = 5L * 1024L * 1024L;
    public static final Set<String> ALLOWED_IMAGE_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "webp");

    public static final String MAIL_USERNAME_PROPERTY = "jpaweb.mail.username";
    public static final String MAIL_APP_PASSWORD_PROPERTY = "jpaweb.mail.appPassword";
    public static final String MAIL_USERNAME_ENV = "JPAWEB_MAIL_USERNAME";
    public static final String MAIL_APP_PASSWORD_ENV = "JPAWEB_MAIL_APP_PASSWORD";

    /**
     * Giá trị này được xác định một lần khi web application khởi động.
     * Có thể cấu hình bằng -Djpaweb.upload.dir hoặc biến môi trường
     * JPAWEB_UPLOAD_DIR.
     */
    public static final String UPLOAD_DIRECTORY = resolveUploadDirectory().toString();

    private Constants() {
    }

    public static Path getUploadDirectory() {
        return Path.of(UPLOAD_DIRECTORY).toAbsolutePath().normalize();
    }

    private static Path resolveUploadDirectory() {
        String configuredDirectory = System.getProperty(UPLOAD_DIRECTORY_PROPERTY);
        if (configuredDirectory == null || configuredDirectory.isBlank()) {
            configuredDirectory = System.getenv(UPLOAD_DIRECTORY_ENV);
        }

        if (configuredDirectory != null && !configuredDirectory.isBlank()) {
            return Path.of(configuredDirectory.trim()).toAbsolutePath().normalize();
        }

        String catalinaBase = System.getProperty("catalina.base");
        if (catalinaBase != null && !catalinaBase.isBlank()) {
            return Path.of(catalinaBase, "uploads", "jpaweb")
                    .toAbsolutePath()
                    .normalize();
        }

        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isBlank()) {
            return Path.of(userHome, "jpaweb-uploads")
                    .toAbsolutePath()
                    .normalize();
        }

        return Path.of(System.getProperty("java.io.tmpdir"), "jpaweb-uploads")
                .toAbsolutePath()
                .normalize();
    }
}
