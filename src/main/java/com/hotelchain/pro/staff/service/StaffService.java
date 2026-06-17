package com.hotelchain.pro.staff.service;

import com.hotelchain.pro.auth.entity.User;
import com.hotelchain.pro.auth.repository.UserRepository;
import com.hotelchain.pro.common.enums.Role;
import com.hotelchain.pro.common.exception.ResourceNotFoundException;
import com.hotelchain.pro.common.exception.HotelChainException;
import com.hotelchain.pro.property.entity.Property;
import com.hotelchain.pro.property.repository.PropertyRepository;
import com.hotelchain.pro.staff.dto.CreateStaffRequest;
import com.hotelchain.pro.staff.dto.UpdateStaffRequest;
import com.hotelchain.pro.staff.dto.StaffDto;
import com.hotelchain.pro.staff.entity.Staff;
import com.hotelchain.pro.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<Object> listStaff(UUID propertyId, Pageable pageable) {
        Page<Staff> page;
        if (propertyId != null) {
            page = staffRepository.findByPropertyId(propertyId, pageable);
        } else {
            page = staffRepository.findAll(pageable);
        }
        return page.map(this::mapToDto);
    }

    @Transactional
    public Object createStaff(CreateStaffRequest request, User user) {
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property", request.getPropertyId().toString()));

        // Kiểm tra xem email đã tồn tại trong User chưa
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new HotelChainException("STAFF_EMAIL_EXISTS", "Email đã tồn tại trong hệ thống");
        }

        // Tạo User account tương ứng cho staff
        User staffUser = new User();
        staffUser.setUsername(request.getEmail());
        staffUser.setEmail(request.getEmail());
        staffUser.setFullName(request.getFullName());
        staffUser.setRole(request.getRole());
        staffUser.setTenantId(property.getTenant().getId());
        staffUser.getAssignedPropertyIds().add(property.getId());
        // Mật khẩu mặc định là Welcome123!
        staffUser.setPassword(passwordEncoder.encode("Welcome123!"));
        staffUser.setMustChangePassword(true);
        User savedUser = userRepository.save(staffUser);

        Staff staff = new Staff();
        staff.setUser(savedUser);
        staff.setProperty(property);
        staff.setFullName(request.getFullName());
        staff.setPhone(request.getPhone());
        staff.setRole(request.getRole());
        staff.setIsActive(true);
        staff.setStartDate(LocalDate.now());
        staff.setAddress(request.getAddress());
        staff.setNotes(request.getNotes());

        Staff savedStaff = staffRepository.save(staff);
        return mapToDto(savedStaff);
    }

    @Transactional
    public Object updateStaff(UUID id, UpdateStaffRequest request) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", id.toString()));

        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property", request.getPropertyId().toString()));

        staff.setFullName(request.getFullName());
        staff.setPhone(request.getPhone());
        staff.setRole(request.getRole());
        staff.setProperty(property);
        staff.setNotes(request.getNotes());
        staff.setAddress(request.getAddress());

        // Cập nhật User account tương ứng
        User staffUser = staff.getUser();
        if (staffUser != null) {
            staffUser.setFullName(request.getFullName());
            staffUser.setRole(request.getRole());
            staffUser.getAssignedPropertyIds().clear();
            staffUser.getAssignedPropertyIds().add(property.getId());
            userRepository.save(staffUser);
        }

        Staff saved = staffRepository.save(staff);
        return mapToDto(saved);
    }

    @Transactional
    public void setActive(UUID id, boolean active) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", id.toString()));
        staff.setIsActive(active);
        if (!active) {
            staff.setEndDate(LocalDate.now());
        } else {
            staff.setEndDate(null);
        }
        staffRepository.save(staff);

        // Khóa hoặc mở tài khoản User tương ứng
        User staffUser = staff.getUser();
        if (staffUser != null) {
            staffUser.setEnabled(active);
            userRepository.save(staffUser);
        }
    }

    @Transactional
    public String resetPassword(UUID id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", id.toString()));

        User staffUser = staff.getUser();
        if (staffUser == null) {
            throw new HotelChainException("USER_NOT_FOUND", "Nhân viên chưa được liên kết với tài khoản đăng nhập");
        }

        String tempPassword = "Temp" + String.format("%06d", (int) (Math.random() * 1000000)) + "!";
        staffUser.setPassword(passwordEncoder.encode(tempPassword));
        staffUser.setMustChangePassword(true);
        userRepository.save(staffUser);

        return tempPassword;
    }

    private StaffDto mapToDto(Staff staff) {
        return StaffDto.builder()
                .id(staff.getId())
                .userId(staff.getUser() != null ? staff.getUser().getId() : null)
                .username(staff.getUser() != null ? staff.getUser().getUsername() : null)
                .propertyId(staff.getProperty().getId())
                .propertyName(staff.getProperty().getName())
                .fullName(staff.getFullName())
                .phone(staff.getPhone())
                .address(staff.getAddress())
                .role(staff.getRole())
                .isActive(staff.getIsActive())
                .startDate(staff.getStartDate())
                .endDate(staff.getEndDate())
                .avatarKey(staff.getAvatarKey())
                .notes(staff.getNotes())
                .build();
    }
}
