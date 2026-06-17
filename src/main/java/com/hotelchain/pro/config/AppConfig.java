package com.hotelchain.pro.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.zxing.BarcodeFormat;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;
import java.util.Optional;
import java.util.UUID;

/**
 * Application-wide bean configuration.
 */
@Slf4j
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AppConfig {

    // ===== MinIO =====

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    @Value("${minio.access-key}")
    private String minioAccessKey;

    @Value("${minio.secret-key}")
    private String minioSecretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioEndpoint)
                .credentials(minioAccessKey, minioSecretKey)
                .build();
    }

    // ===== Firebase FCM =====

    @Value("${notification.firebase.credentials-file:}")
    private String firebaseCredentialsFile;

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        try {
            if (firebaseCredentialsFile != null && !firebaseCredentialsFile.isBlank()
                    && FirebaseApp.getApps().isEmpty()) {
                FileInputStream serviceAccount = new FileInputStream(firebaseCredentialsFile);
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(com.google.auth.oauth2.GoogleCredentials.fromStream(serviceAccount))
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized from credentials file");
            }
            return FirebaseMessaging.getInstance();
        } catch (Exception e) {
            log.warn("Firebase not initialized (credentials missing): {}", e.getMessage());
            return null; // Allow startup without Firebase in dev
        }
    }

    // ===== Google Authenticator (2FA) =====

    @Bean
    public GoogleAuthenticator googleAuthenticator() {
        return new GoogleAuthenticator();
    }

    // ===== JPA Auditing =====

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }
            if (authentication.getPrincipal() instanceof com.hotelchain.pro.auth.entity.User user) {
                return Optional.of(user.getId());
            }
            return Optional.empty();
        };
    }

    // ===== Jackson =====

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    // ===== RestTemplate =====

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
