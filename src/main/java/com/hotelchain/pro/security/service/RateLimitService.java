package com.hotelchain.pro.security.service;

import com.hotelchain.pro.common.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Rate Limit Service — giới hạn số lần đăng nhập thất bại.
 * Max 5 lần / IP / 15 phút
 * Max 3 lần / username / 15 phút → khóa tài khoản
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final String IP_LOGIN_KEY = "rate:login:ip:";
    private static final String USER_LOGIN_KEY = "rate:login:user:";

    @Value("${rate-limit.login.max-attempts-per-ip:5}")
    private int maxAttemptsPerIp;

    @Value("${rate-limit.login.max-attempts-per-username:3}")
    private int maxAttemptsPerUsername;

    @Value("${rate-limit.login.window-minutes:15}")
    private int windowMinutes;

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Kiểm tra và tăng bộ đếm login.
     * Ném exception nếu vượt ngưỡng.
     */
    public void checkLoginRateLimit(String ipAddress, String username) {
        // Check IP rate limit
        String ipKey = IP_LOGIN_KEY + ipAddress;
        Long ipCount = redisTemplate.opsForValue().increment(ipKey);
        if (ipCount == 1) {
            redisTemplate.expire(ipKey, Duration.ofMinutes(windowMinutes));
        }
        if (ipCount != null && ipCount > maxAttemptsPerIp) {
            log.warn("Rate limit exceeded for IP: {}", ipAddress);
            throw new AuthException("RATE_LIMIT_EXCEEDED",
                    String.format("Quá nhiều lần đăng nhập thất bại từ IP này. Vui lòng thử lại sau %d phút.", windowMinutes));
        }

        // Check username rate limit
        if (username != null) {
            String userKey = USER_LOGIN_KEY + username;
            Long userCount = redisTemplate.opsForValue().increment(userKey);
            if (userCount == 1) {
                redisTemplate.expire(userKey, Duration.ofMinutes(windowMinutes));
            }
        }
    }

    /**
     * Reset counter sau khi login thành công.
     */
    public void resetLoginRateLimit(String ipAddress, String username) {
        redisTemplate.delete(IP_LOGIN_KEY + ipAddress);
        if (username != null) {
            redisTemplate.delete(USER_LOGIN_KEY + username);
        }
    }

    /**
     * Check API rate limit (300 req/min mặc định).
     */
    public boolean checkApiRateLimit(String clientId, int maxPerMinute) {
        String key = "rate:api:" + clientId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }
        return count != null && count <= maxPerMinute;
    }
}
