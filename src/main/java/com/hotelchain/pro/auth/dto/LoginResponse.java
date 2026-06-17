package com.hotelchain.pro.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private long expiresIn;     // Giây

    private UserInfo user;

    @Data
    @Builder
    public static class UserInfo {
        private UUID id;
        private String fullName;
        private String email;
        private String role;
        private List<PropertyInfo> assignedProperties;
        private String avatarUrl;
        private boolean mustChangePassword;
    }

    @Data
    @Builder
    public static class PropertyInfo {
        private UUID id;
        private String name;
        private String code;
    }
}
