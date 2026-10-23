package com.foodbridge.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * Validates and stores a food photo.
     *
     * @return the public URL (e.g. /uploads/food/ab12.jpg), or null if no file was provided
     */
    String store(MultipartFile file);

    /** Removes a previously stored file (ignored if the URL is null or not one of ours). */
    void delete(String url);
}
