package com.hotelchain.pro.controller;

import com.hotelchain.pro.dto.ApiResponse;
import com.hotelchain.pro.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileController {

    private final FileUploadService fileUploadService;

    @PostMapping("/photo")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "utility") String type) {

        if (file.isEmpty()) {
            throw new RuntimeException("File không được để trống");
        }

        String objectKey = fileUploadService.uploadPhoto(file, type);
        String url = fileUploadService.getPresignedUrl(objectKey);

        return ResponseEntity.ok(ApiResponse.ok("Upload thành công",
                Map.of("objectKey", objectKey, "url", url != null ? url : "")));
    }

    @GetMapping("/url")
    public ResponseEntity<ApiResponse<Map<String, String>>> getUrl(@RequestParam String key) {
        String url = fileUploadService.getPresignedUrl(key);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("url", url != null ? url : "")));
    }

    @GetMapping("/serve")
    public ResponseEntity<org.springframework.core.io.Resource> serveFile(@RequestParam String key) {
        try {
            java.nio.file.Path file = java.nio.file.Paths.get("uploads/" + key);
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(file.toUri());
            
            if (resource.exists() || resource.isReadable()) {
                String contentType = "image/jpeg";
                if (key.endsWith(".png")) contentType = "image/png";
                
                return ResponseEntity.ok()
                        .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, contentType)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (java.net.MalformedURLException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
