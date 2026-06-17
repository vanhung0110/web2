package com.hotelchain.pro.security.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelchain.pro.auth.entity.User;
import com.hotelchain.pro.security.annotation.Auditable;
import com.hotelchain.pro.security.entity.AuditLog;
import com.hotelchain.pro.security.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * AuditAspect — tự động ghi audit log cho các method được đánh dấu @Auditable.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void audit(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setAction(auditable.action());
            auditLog.setEntityType(auditable.entityType());
            auditLog.setTimestamp(LocalDateTime.now());

            // Lấy thông tin user hiện tại
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof User user) {
                auditLog.setUserId(user.getId());
                auditLog.setUsername(user.getUsername());
                auditLog.setTenantId(user.getTenantId());
            }

            // Lấy thông tin request
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                auditLog.setIpAddress(getClientIp(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
                auditLog.setRequestUri(request.getRequestURI());
            }

            // Serialize result thành newValue nếu cần
            if (result != null && auditable.captureOldValue()) {
                try {
                    auditLog.setNewValue(objectMapper.writeValueAsString(result));
                } catch (Exception e) {
                    log.debug("Could not serialize audit value: {}", e.getMessage());
                }
            }

            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            log.error("Audit logging failed: {}", e.getMessage());
            // Không throw để không ảnh hưởng business logic
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
