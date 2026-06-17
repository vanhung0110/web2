package com.hotelchain.pro.auth.jwt;

import com.hotelchain.pro.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JWT Token Provider — tạo, xác thực, revoke token.
 *
 * Claims chuẩn:
 * - sub: userId
 * - roles: ["RECEPTIONIST"]
 * - tenantId: UUID
 * - propertyIds: [UUID, UUID]
 * - deviceId: UUID
 * - iat, exp, jti (token ID cho blacklist)
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TENANT_ID = "tenantId";
    private static final String CLAIM_PROPERTY_IDS = "propertyIds";
    private static final String CLAIM_DEVICE_ID = "deviceId";
    private static final String REDIS_TOKEN_BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String REDIS_USER_TOKENS_PREFIX = "jwt:user:";

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.access-token-expiry:1800}")
    private long accessTokenExpirySeconds;

    @Value("${security.jwt.refresh-token-expiry:604800}")
    private long refreshTokenExpirySeconds;

    private final RedisTemplate<String, String> redisTemplate;

    public JwtTokenProvider(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(Base64.getEncoder().encodeToString(jwtSecret.getBytes()));
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Sinh Access Token (30 phút).
     */
    public String generateAccessToken(User user) {
        return generateAccessToken(user, null, null);
    }

    public String generateAccessToken(User user, String deviceId, String fcmToken) {
        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();

        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_ROLES, List.of("ROLE_" + user.getRole().name()));
        if (user.getTenantId() != null) {
            claims.put(CLAIM_TENANT_ID, user.getTenantId().toString());
        }
        if (user.getAssignedPropertyIds() != null && !user.getAssignedPropertyIds().isEmpty()) {
            claims.put(CLAIM_PROPERTY_IDS, user.getAssignedPropertyIds().stream()
                    .map(UUID::toString)
                    .collect(Collectors.toList()));
        }
        if (deviceId != null) {
            claims.put(CLAIM_DEVICE_ID, deviceId);
        }

        String token = Jwts.builder()
                .claims(claims)
                .subject(user.getId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenExpirySeconds)))
                .id(jti)
                .signWith(getSigningKey())
                .compact();

        // Track active tokens per user for logout-all
        redisTemplate.opsForSet().add(REDIS_USER_TOKENS_PREFIX + user.getId(), jti);
        redisTemplate.expire(REDIS_USER_TOKENS_PREFIX + user.getId(),
                Duration.ofSeconds(accessTokenExpirySeconds));

        return token;
    }

    /**
     * Sinh Refresh Token (7 ngày) — lưu Redis.
     */
    public String generateRefreshToken(String userId, String deviceId) {
        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();

        String token = Jwts.builder()
                .subject(userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTokenExpirySeconds)))
                .id(jti)
                .claim("type", "refresh")
                .claim(CLAIM_DEVICE_ID, deviceId != null ? deviceId : "")
                .signWith(getSigningKey())
                .compact();

        // Lưu refresh token vào Redis (có thể revoke)
        redisTemplate.opsForValue().set(
                "refresh:" + jti,
                userId,
                Duration.ofSeconds(refreshTokenExpirySeconds)
        );

        return token;
    }

    /**
     * Revoke 1 token (thêm vào blacklist Redis).
     */
    public void revokeToken(String jti) {
        redisTemplate.opsForValue().set(
                REDIS_TOKEN_BLACKLIST_PREFIX + jti,
                "1",
                Duration.ofSeconds(accessTokenExpirySeconds)
        );
        // Xóa refresh token nếu có
        redisTemplate.delete("refresh:" + jti);
    }

    /**
     * Revoke tất cả token của một user (logout all devices).
     */
    public void revokeAllUserTokens(UUID userId) {
        Set<String> jtis = redisTemplate.opsForSet().members(REDIS_USER_TOKENS_PREFIX + userId.toString());
        if (jtis != null && !jtis.isEmpty()) {
            for (String jti : jtis) {
                redisTemplate.opsForValue().set(
                        REDIS_TOKEN_BLACKLIST_PREFIX + jti,
                        "1",
                        Duration.ofSeconds(accessTokenExpirySeconds)
                );
            }
        }
        redisTemplate.delete(REDIS_USER_TOKENS_PREFIX + userId.toString());
    }

    /**
     * Kiểm tra token có trong blacklist không.
     */
    public boolean isTokenRevoked(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(REDIS_TOKEN_BLACKLIST_PREFIX + jti));
    }

    /**
     * Parse và validate JWT, trả về Claims.
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new com.hotelchain.pro.common.exception.AuthException("AUTH_002", "Token hết hạn");
        } catch (JwtException e) {
            throw new com.hotelchain.pro.common.exception.AuthException("AUTH_003", "Token không hợp lệ");
        }
    }

    public String getUserIdFromToken(String token) {
        return parseToken(token).getSubject();
    }

    public String getJtiFromToken(String token) {
        return parseToken(token).getId();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = parseToken(token);
            String jti = claims.getId();
            if (isTokenRevoked(jti)) return false;
            return claims.getSubject().equals(((User) userDetails).getId().toString());
        } catch (Exception e) {
            return false;
        }
    }

    public long getAccessTokenExpirySeconds() {
        return accessTokenExpirySeconds;
    }
}
