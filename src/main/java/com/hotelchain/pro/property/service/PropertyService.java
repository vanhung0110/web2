package com.hotelchain.pro.property.service;

import com.hotelchain.pro.auth.entity.User;
import com.hotelchain.pro.common.enums.Role;
import com.hotelchain.pro.common.exception.ResourceNotFoundException;
import com.hotelchain.pro.property.dto.CreatePropertyRequest;
import com.hotelchain.pro.property.dto.PropertyDto;
import com.hotelchain.pro.property.dto.UpdatePropertyRequest;
import com.hotelchain.pro.property.entity.Property;
import com.hotelchain.pro.property.entity.Tenant;
import com.hotelchain.pro.property.repository.PropertyRepository;
import com.hotelchain.pro.property.repository.TenantRepository;
import com.hotelchain.pro.storage.service.MinioStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final TenantRepository tenantRepository;
    private final MinioStorageService storageService;

    public List<PropertyDto> listProperties(User user) {
        List<Property> properties;
        if (user.getRole() == Role.SUPER_ADMIN) {
            properties = propertyRepository.findAll();
        } else if (user.getTenantId() != null) {
            properties = propertyRepository.findByTenantId(user.getTenantId());
        } else {
            properties = List.of();
        }
        return properties.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public PropertyDto createProperty(CreatePropertyRequest request, User user) {
        Tenant tenant = null;
        if (user.getTenantId() != null) {
            tenant = tenantRepository.findById(user.getTenantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tenant", user.getTenantId().toString()));
        } else {
            // Cho phép Super Admin tạo chi nhánh cho Tenant đầu tiên hoặc tạo mặc định
            List<Tenant> tenants = tenantRepository.findAll();
            if (!tenants.isEmpty()) {
                tenant = tenants.get(0);
            } else {
                throw new com.hotelchain.pro.common.exception.HotelChainException("TENANT_REQUIRED", "Không tìm thấy Tenant nào trong hệ thống");
            }
        }

        Property property = new Property();
        property.setTenant(tenant);
        property.setName(request.getName());
        property.setCode(request.getCode());
        property.setAddress(request.getAddress());
        property.setWard(request.getWard());
        property.setDistrict(request.getDistrict());
        property.setCity(request.getCity());
        property.setLatitude(request.getLatitude());
        property.setLongitude(request.getLongitude());
        property.setPhone(request.getPhone());
        property.setEmail(request.getEmail());
        property.setDescription(request.getDescription());
        property.setType(request.getType());
        property.setStarRating(request.getStarRating());
        property.setIsActive(true);

        Property saved = propertyRepository.save(property);
        return mapToDto(saved);
    }

    public PropertyDto getProperty(UUID id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property", id.toString()));
        return mapToDto(property);
    }

    @Transactional
    public PropertyDto updateProperty(UUID id, UpdatePropertyRequest request) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property", id.toString()));

        property.setName(request.getName());
        property.setAddress(request.getAddress());
        property.setWard(request.getWard());
        property.setDistrict(request.getDistrict());
        property.setCity(request.getCity());
        property.setLatitude(request.getLatitude());
        property.setLongitude(request.getLongitude());
        property.setPhone(request.getPhone());
        property.setEmail(request.getEmail());
        property.setDescription(request.getDescription());
        property.setType(request.getType());
        property.setStarRating(request.getStarRating());

        Property saved = propertyRepository.save(property);
        return mapToDto(saved);
    }

    @Transactional
    public void deactivateProperty(UUID id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property", id.toString()));
        property.setIsActive(false);
        propertyRepository.save(property);
    }

    @Transactional
    public List<String> uploadImages(UUID id, List<MultipartFile> files) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property", id.toString()));

        List<String> keys = files.stream()
                .map(file -> storageService.uploadFile(file, "properties/" + id))
                .collect(Collectors.toList());

        property.getImageKeys().addAll(keys);
        propertyRepository.save(property);
        return keys;
    }

    public Object getPropertyDashboard(UUID id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property", id.toString()));

        // Trả về map thông tin dashboard mẫu
        return Map.of(
                "propertyId", property.getId(),
                "propertyName", property.getName(),
                "today", Map.of(
                        "checkIns", 0,
                        "checkOuts", 0,
                        "newBookings", 0,
                        "revenue", 0
                ),
                "occupancy", Map.of(
                        "totalRooms", 0,
                        "occupiedRooms", 0,
                        "availableRooms", 0,
                        "occupancyRate", 0.0
                )
        );
    }

    public Object getOccupancy(UUID id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property", id.toString()));

        return Map.of(
                "propertyId", property.getId(),
                "propertyName", property.getName(),
                "occupancyRate", 0.0,
                "totalRooms", 0,
                "occupiedRooms", 0
        );
    }

    private PropertyDto mapToDto(Property property) {
        return PropertyDto.builder()
                .id(property.getId())
                .name(property.getName())
                .code(property.getCode())
                .address(property.getAddress())
                .ward(property.getWard())
                .district(property.getDistrict())
                .city(property.getCity())
                .latitude(property.getLatitude())
                .longitude(property.getLongitude())
                .phone(property.getPhone())
                .email(property.getEmail())
                .description(property.getDescription())
                .type(property.getType())
                .starRating(property.getStarRating())
                .isActive(property.getIsActive())
                .imageKeys(property.getImageKeys())
                .build();
    }
}
