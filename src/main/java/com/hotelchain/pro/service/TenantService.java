package com.hotelchain.pro.service;

import com.hotelchain.pro.dto.CreateTenantRequest;
import com.hotelchain.pro.entity.Room;
import com.hotelchain.pro.entity.Tenant;
import com.hotelchain.pro.entity.User;
import com.hotelchain.pro.enums.Role;
import com.hotelchain.pro.repository.RoomRepository;
import com.hotelchain.pro.repository.TenantRepository;
import com.hotelchain.pro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final TenantRepository tenantRepository;

    public List<Tenant> getAllActiveTenants() {
        return tenantRepository.findByIsActiveTrue();
    }

    /**
     * Admin tạo người thuê mới:
     * 1. Tạo user (nếu chưa có)
     * 2. Gán phòng
     * 3. Đánh dấu phòng là occupied
     */
    @Transactional
    public Tenant createTenant(CreateTenantRequest request) {
        String phone = request.getPhone().trim().replaceAll("[^0-9]", "");

        // Kiểm tra phòng
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("Phòng không tồn tại"));

        if (room.getIsOccupied()) {
            throw new RuntimeException("Phòng " + room.getRoomNumber() + " đã có người thuê");
        }

        // Tìm hoặc tạo user
        User user = userRepository.findByPhone(phone).orElse(null);
        if (user == null) {
            user = new User();
            user.setPhone(phone);
            user.setFullName(request.getFullName());
            user.setRole(Role.USER);
            user.setIsActive(true);
            user = userRepository.save(user);
        } else {
            // Kiểm tra user đã có phòng chưa
            if (tenantRepository.findByUserIdAndIsActiveTrue(user.getId()).isPresent()) {
                throw new RuntimeException("Người dùng này đã được gán phòng khác");
            }
        }

        // Tạo tenant
        Tenant tenant = new Tenant();
        tenant.setUser(user);
        tenant.setRoom(room);
        tenant.setMoveInDate(LocalDate.now());
        tenant.setIsActive(true);
        tenant = tenantRepository.save(tenant);

        // Đánh dấu phòng occupied
        room.setIsOccupied(true);
        roomRepository.save(room);

        return tenant;
    }

    /**
     * Admin xóa người thuê (trả phòng).
     */
    @Transactional
    public void removeTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Người thuê không tồn tại"));

        tenant.setIsActive(false);
        tenant.setMoveOutDate(LocalDate.now());
        tenantRepository.save(tenant);

        // Trả phòng về trạng thái trống
        Room room = tenant.getRoom();
        room.setIsOccupied(false);
        roomRepository.save(room);
    }
}
