package vn.hcmute.jpaweb.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.hcmute.jpaweb.utils.ImageStorage;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

@WebServlet(name = "ImageController", urlPatterns = "/image")
public class ImageController extends HttpServlet {

    private transient ImageStorage imageStorage;

    @Override
    public void init() throws ServletException {
        imageStorage = new ImageStorage();
        try {
            imageStorage.ensureUploadDirectory();
        } catch (IOException exception) {
            throw new ServletException("Không thể khởi tạo thư mục chứa ảnh.", exception);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String fileName = request.getParameter("fname");
        Path imagePath = imageStorage.resolveForRead(fileName);

        if (imagePath == null
                || !Files.isRegularFile(imagePath, LinkOption.NOFOLLOW_LINKS)
                || !Files.isReadable(imagePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(ImageStorage.contentType(fileName));
        response.setContentLengthLong(Files.size(imagePath));
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "public, max-age=31536000, immutable");
        response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");

        try (OutputStream output = response.getOutputStream()) {
            Files.copy(imagePath, output);
        }
    }
}
