package com.hotelchain.pro.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.time.Duration;

/**
 * Payment Security — xác minh webhook ngân hàng, idempotency.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSecurityService {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Xác minh webhook từ ngân hàng bằng HMAC-SHA256.
     * So sánh constant-time để chống timing attack.
     */
    public boolean verifyWebhookSignature(String payload, String signature, String secret) {
        try {
            String expected = HmacUtils.hmacSha256Hex(secret, payload);
            return MessageDigest.isEqual(
                    expected.getBytes(),
                    signature.getBytes()
            );
        } catch (Exception e) {
            log.error("Webhook signature verification error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Idempotency key — tránh xử lý trùng webhook.
     */
    public boolean isAlreadyProcessed(String transactionId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("txn:" + transactionId));
    }

    /**
     * Đánh dấu transaction đã xử lý, giữ 7 ngày.
     */
    public void markAsProcessed(String transactionId) {
        redisTemplate.opsForValue().set(
                "txn:" + transactionId,
                "1",
                Duration.ofDays(7)
        );
    }
}
