package vn.hcmute.jpaweb.utils;

import jakarta.servlet.http.Part;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Lưu và xóa ảnh local bên ngoài thư mục triển khai WAR.
 */
public class ImageStorage {

    private static final Pattern SAFE_FILE_NAME =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,199}");

    private final Path uploadDirectory;

    public ImageStorage() {
        this(Constants.getUploadDirectory());
    }

    ImageStorage(Path uploadDirectory) {
        this.uploadDirectory = uploadDirectory.toAbsolutePath().normalize();
    }

    public void ensureUploadDirectory() throws IOException {
        Files.createDirectories(uploadDirectory);
        if (!Files.isDirectory(uploadDirectory) || !Files.isWritable(uploadDirectory)) {
            throw new IOException("Thư mục upload không tồn tại hoặc không thể ghi.");
        }
    }

    public String save(Part imagePart) throws IOException {
        if (!hasUpload(imagePart)) {
            return null;
        }
        if (imagePart.getSize() > Constants.MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("Ảnh tải lên không được vượt quá 5 MB.");
        }

        String originalName = imagePart.getSubmittedFileName();
        String extension = getExtension(originalName);
        if (!Constants.ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Chỉ chấp nhận ảnh JPG, JPEG, PNG hoặc WEBP.");
        }

        ensureUploadDirectory();
        String storedName = UUID.randomUUID() + "." + extension;
        Path target = uploadDirectory.resolve(storedName).normalize();
        if (!target.getParent().equals(uploadDirectory)) {
            throw new IOException("Đường dẫn lưu ảnh không hợp lệ.");
        }

        try (InputStream rawInput = imagePart.getInputStream();
             BufferedInputStream input = new BufferedInputStream(rawInput)) {
            input.mark(16);
            byte[] header = input.readNBytes(12);
            input.reset();
            if (!hasExpectedSignature(header, extension)) {
                throw new IllegalArgumentException("Nội dung file không đúng định dạng ảnh đã chọn.");
            }
            Files.copy(input, target);
        } catch (IOException | RuntimeException exception) {
            Files.deleteIfExists(target);
            throw exception;
        }
        return storedName;
    }

    public void deleteIfLocal(String imageName) throws IOException {
        if (!isSafeLocalFileName(imageName)) {
            return;
        }

        Path target = uploadDirectory.resolve(imageName).normalize();
        if (target.getParent().equals(uploadDirectory)) {
            Files.deleteIfExists(target);
        }
    }

    public Path resolveForRead(String imageName) {
        if (!isSafeLocalFileName(imageName)) {
            return null;
        }
        Path target = uploadDirectory.resolve(imageName).normalize();
        return target.getParent().equals(uploadDirectory) ? target : null;
    }

    public static boolean hasUpload(Part imagePart) {
        return imagePart != null
                && imagePart.getSize() > 0
                && imagePart.getSubmittedFileName() != null
                && !imagePart.getSubmittedFileName().isBlank();
    }

    public static boolean isRemoteImage(String image) {
        if (image == null) {
            return false;
        }
        String lowerCaseImage = image.trim().toLowerCase(Locale.ROOT);
        return lowerCaseImage.startsWith("http://") || lowerCaseImage.startsWith("https://");
    }

    public static boolean isSafeLocalFileName(String imageName) {
        if (imageName == null || imageName.isBlank()
                || Constants.DEFAULT_IMAGE.equals(imageName)
                || isRemoteImage(imageName)
                || imageName.contains("..")
                || !SAFE_FILE_NAME.matcher(imageName).matches()) {
            return false;
        }

        String extension = getExtension(imageName);
        return Constants.ALLOWED_IMAGE_EXTENSIONS.contains(extension);
    }

    public static String contentType(String imageName) {
        return switch (getExtension(imageName)) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    private static String getExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        String normalizedName = fileName.replace('\\', '/');
        normalizedName = normalizedName.substring(normalizedName.lastIndexOf('/') + 1);
        int dotIndex = normalizedName.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == normalizedName.length() - 1) {
            return "";
        }
        return normalizedName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean hasExpectedSignature(byte[] header, String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> header.length >= 3
                    && (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF;
            case "png" -> header.length >= 8
                    && Arrays.equals(Arrays.copyOf(header, 8), new byte[]{
                    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
            });
            case "webp" -> header.length >= 12
                    && header[0] == 'R' && header[1] == 'I'
                    && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E'
                    && header[10] == 'B' && header[11] == 'P';
            default -> false;
        };
    }
}
