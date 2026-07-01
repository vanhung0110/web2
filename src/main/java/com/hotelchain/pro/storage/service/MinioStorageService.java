package com.hotelchain.pro.storage.service;

import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO Storage Service — upload, download, presigned URL, delete.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    @Value("${minio.bucket-name:hotel-uploads}")
    private String bucketName;

    private final MinioClient minioClient;

    @PostConstruct
    public void init() {
        ensureBucketExists();
    }

    /**
     * Upload MultipartFile lên MinIO.
     * @return object key
     */
    public String uploadFile(MultipartFile file, String folder) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String objectKey = folder + "/" + UUID.randomUUID() + extension;

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                    .build());

            return objectKey;
        } catch (Exception e) {
            log.error("Error uploading file to MinIO: {}", e.getMessage(), e);
            throw new com.hotelchain.pro.common.exception.HotelChainException(
                    "STORAGE_ERROR", "Lưu file thất bại: " + e.getMessage());
        }
    }

    /**
     * Upload bytes với metadata.
     */
    public void uploadBytesWithMetadata(String objectKey, byte[] bytes, String contentType, Map<String, String> metadata) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType(contentType)
                    .userMetadata(metadata)
                    .build());
        } catch (Exception e) {
            log.error("Error uploading bytes to MinIO: {}", e.getMessage(), e);
            throw new com.hotelchain.pro.common.exception.HotelChainException(
                    "STORAGE_ERROR", "Lưu file thất bại");
        }
    }

    /**
     * Upload bytes đơn giản.
     */
    public void uploadBytes(String objectKey, byte[] bytes, String contentType) {
        uploadBytesWithMetadata(objectKey, bytes, contentType, Map.of());
    }

    /**
     * Sinh presigned URL để truy cập file tạm thời (1 giờ).
     */
    public String getPresignedUrl(String objectKey) {
        return getPresignedUrl(objectKey, 3600);
    }

    public String getPresignedUrl(String objectKey, int expirySeconds) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .method(Method.GET)
                    .expiry(expirySeconds, TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            log.error("Error generating presigned URL: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Xóa object khỏi MinIO.
     */
    public void deleteObject(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.warn("Error deleting object from MinIO: {}", e.getMessage());
        }
    }

    /**
     * Lấy InputStream của object.
     */
    public InputStream getObject(String objectKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.error("Error getting object from MinIO: {}", e.getMessage());
            throw new com.hotelchain.pro.common.exception.ResourceNotFoundException("File", objectKey);
        }
    }

    /**
     * Kiểm tra bucket tồn tại, tạo mới nếu chưa có.
     */
    public void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created MinIO bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Error checking/creating MinIO bucket: {}", e.getMessage());
        }
    }
}
