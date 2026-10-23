package com.foodbridge.service.impl;

import com.foodbridge.exception.BusinessException;
import com.foodbridge.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Stores images on local disk under {app.upload-dir}/food and exposes them at /uploads/food/**.
 * The file type is detected from the file's first bytes (not from the client-supplied name or content type),
 * and the stored name is a random UUID, so a user can never choose the path or upload a script.
 */
@Service
public class LocalFileStorageService implements FileStorageService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final String URL_PREFIX = "/uploads/food/";

    private final Path root;

    public LocalFileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("food");
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create upload directory " + root, e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException("The image is too large (maximum 5 MB).");
        }
        try {
            byte[] head;
            try (InputStream in = file.getInputStream()) {
                head = in.readNBytes(12);
            }
            String ext = detectExtension(head);
            if (ext == null) {
                throw new BusinessException("Only JPG, PNG, GIF or WebP images are allowed.");
            }
            String name = UUID.randomUUID() + "." + ext;
            Path target = root.resolve(name).normalize();
            if (!target.startsWith(root)) {
                throw new BusinessException("Invalid file name.");
            }
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target);
            }
            return URL_PREFIX + name;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store the uploaded image", e);
        }
    }

    @Override
    public void delete(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) {
            return;
        }
        Path p = root.resolve(url.substring(URL_PREFIX.length())).normalize();
        if (p.startsWith(root)) {
            try {
                Files.deleteIfExists(p);
            } catch (IOException ignored) {
                // best effort clean-up
            }
        }
    }

    private static String detectExtension(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "png";
        }
        if (b.length >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') {
            return "gif";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "webp";
        }
        return null;
    }
}
