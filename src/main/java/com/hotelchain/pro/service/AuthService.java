package com.hotelchain.pro.service;

import com.hotelchain.pro.dto.LoginRequest;
import com.hotelchain.pro.dto.LoginResponse;
import com.hotelchain.pro.entity.Tenant;
import com.hotelchain.pro.entity.User;
import com.hotelchain.pro.repository.TenantRepository;
import com.hotelchain.pro.repository.UserRepository;
import com.hotelchain.pro.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final JwtUtils jwtUtils;

    /**
     * Đăng nhập bằng số điện thoại — không cần mật khẩu.
     * - Nếu SĐT là admin → trả role ADMIN
     * - Nếu SĐT là user → trả role USER + thông tin phòng
     * - Nếu không tồn tại → báo lỗi
     */
    public LoginResponse login(LoginRequest request) {
        String phone = normalizePhone(request.getPhone());

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Số điện thoại chưa được đăng ký trong hệ thống"));

        if (!user.getIsActive()) {
            throw new RuntimeException("Tài khoản đã bị khóa");
        }

        // Tạo JWT Token
        String token = jwtUtils.generateToken(user.getId(), user.getRole().name());

        LoginResponse.LoginResponseBuilder builder = LoginResponse.builder()
                .userId(user.getId())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .token(token);

        // Nếu là USER, tìm phòng đang thuê
        if (user.getRole() == com.hotelchain.pro.enums.Role.USER) {
            tenantRepository.findByUserIdAndIsActiveTrue(user.getId())
                    .ifPresent(tenant -> {
                        builder.roomId(tenant.getRoom().getId());
                        builder.roomNumber(tenant.getRoom().getRoomNumber());
                    });
        }

        return builder.build();
    }

    private String normalizePhone(String phone) {
        if (phone == null) throw new RuntimeException("Số điện thoại không được để trống");
        return phone.trim().replaceAll("[^0-9]", "");
    }
}
