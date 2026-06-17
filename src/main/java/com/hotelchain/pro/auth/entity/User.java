package com.hotelchain.pro.auth.entity;

import com.hotelchain.pro.common.entity.BaseEntity;
import com.hotelchain.pro.common.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;

/**
 * User — Tài khoản đăng nhập hệ thống.
 * Implements UserDetails cho Spring Security.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_username", columnList = "username", unique = true),
        @Index(name = "idx_users_email", columnList = "email", unique = true),
        @Index(name = "idx_users_tenant", columnList = "tenant_id")
})
@Getter
@Setter
public class User extends BaseEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;        // Email hoặc username

    @Column(nullable = false)
    private String password;        // BCrypt hashed

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "tenant_id")
    private UUID tenantId;

    // Danh sách chi nhánh được phép truy cập
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_property_access", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "property_id")
    private Set<UUID> assignedPropertyIds = new HashSet<>();

    private String avatarKey;       // MinIO key

    // Account security
    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private Boolean accountLocked = false;

    private Integer failedLoginAttempts = 0;
    private LocalDateTime lockedUntil;

    private LocalDateTime lastLoginAt;
    private String lastLoginIp;

    // Password policy
    private LocalDateTime passwordChangedAt;
    private Boolean mustChangePassword = false; // Force change on next login

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_password_history", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "password_hash")
    @OrderColumn(name = "seq")
    private List<String> passwordHistory = new ArrayList<>(); // 5 mật khẩu gần nhất

    // 2FA TOTP
    private Boolean twoFactorEnabled = false;
    private String twoFactorSecret;             // Base32 encoded secret

    // Device tokens for push notifications
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_fcm_tokens", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "fcm_token")
    private Set<String> fcmTokens = new HashSet<>();

    // ===== UserDetails Implementation =====

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        if (accountLocked && lockedUntil != null) {
            return LocalDateTime.now().isAfter(lockedUntil);
        }
        return !accountLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
