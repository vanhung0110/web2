package com.hotelchain.pro.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileUploadService {

    private final String uploadDir = "uploads/";

    public FileUploadService() {
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public String uploadPhoto(MultipartFile file, String prefix) {
        try {
            String ext = getExtension(file.getOriginalFilename());
            String objectKey = prefix + "_" + UUID.randomUUID() + ext;
            
            Path targetPath = Paths.get(uploadDir + objectKey);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Uploaded file locally: {}", objectKey);
            return objectKey;
        } catch (IOException e) {
            log.error("Upload failed: {}", e.getMessage());
            throw new RuntimeException("Upload ảnh thất bại: " + e.getMessage());
        }
    }

    public String getPresignedUrl(String objectKey) {
        if (objectKey == null || objectKey.isEmpty()) return null;
        // Return a local URL that FileController will serve
        return "/api/upload/serve?key=" + objectKey;
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot) : ".jpg";
    }
}
