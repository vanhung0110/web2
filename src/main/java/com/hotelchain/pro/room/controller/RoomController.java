package com.hotelchain.pro.room.controller;

import com.hotelchain.pro.common.enums.RoomStatus;
import com.hotelchain.pro.common.enums.RoomView;
import com.hotelchain.pro.common.exception.ResourceNotFoundException;
import com.hotelchain.pro.common.response.ApiResponse;
import com.hotelchain.pro.property.entity.Property;
import com.hotelchain.pro.property.repository.PropertyRepository;
import com.hotelchain.pro.room.entity.Room;
import com.hotelchain.pro.room.entity.RoomType;
import com.hotelchain.pro.room.repository.RoomRepository;
import com.hotelchain.pro.room.repository.RoomTypeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "Rooms", description = "Quản lý phòng và loại phòng")
@RestController
@RequiredArgsConstructor
public class RoomController {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final PropertyRepository propertyRepository;

    private static final String RECEPTIONIST_ROLES =
            "hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'ACCOUNTANT')";

    private static final String MANAGER_ROLES =
            "hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER')";

    // ===== Room Types API =====

    @Operation(summary = "Danh sách loại phòng của chi nhánh")
    @GetMapping("/api/v1/properties/{propertyId}/room-types")
    @PreAuthorize(RECEPTIONIST_ROLES)
    public ResponseEntity<ApiResponse<List<RoomTypeDto>>> getRoomTypes(@PathVariable UUID propertyId) {
        List<RoomType> types = roomTypeRepository.findByPropertyIdAndIsActiveTrue(propertyId);
        List<RoomTypeDto> dtos = types.stream()
                .map(t -> new RoomTypeDto(t.getId(), t.getName(), t.getCode(), t.getBasePrice(), t.getMaxOccupancy()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @Operation(summary = "Tạo loại phòng mới")
    @PostMapping("/api/v1/properties/{propertyId}/room-types")
    @PreAuthorize(MANAGER_ROLES)
    public ResponseEntity<ApiResponse<RoomTypeDto>> createRoomType(
            @PathVariable UUID propertyId,
            @RequestBody CreateRoomTypeRequest request) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property", propertyId.toString()));

        RoomType roomType = new RoomType();
        roomType.setProperty(property);
        roomType.setName(request.name());
        roomType.setCode(request.code());
        roomType.setBasePrice(request.basePrice());
        roomType.setMaxOccupancy(request.maxOccupancy());
        roomType.setBedCount(1);
        roomType.setBedType("DOUBLE");
        roomType.setArea(25.0);
        roomType.setIsActive(true);

        RoomType saved = roomTypeRepository.save(roomType);
        return ResponseEntity.ok(ApiResponse.created(new RoomTypeDto(
                saved.getId(), saved.getName(), saved.getCode(), saved.getBasePrice(), saved.getMaxOccupancy()
        )));
    }

    // ===== Rooms API =====

    @Operation(summary = "Danh sách phòng của chi nhánh")
    @GetMapping("/api/v1/properties/{propertyId}/rooms")
    @PreAuthorize(RECEPTIONIST_ROLES)
    public ResponseEntity<ApiResponse<List<RoomDto>>> getRooms(@PathVariable UUID propertyId) {
        List<Room> rooms = roomRepository.findByPropertyIdAndDeletedFalseOrderByFloorAscRoomNumberAsc(propertyId);
        List<RoomDto> dtos = rooms.stream().map(this::mapToDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @Operation(summary = "Sơ đồ phòng theo tầng")
    @GetMapping("/api/v1/properties/{propertyId}/rooms/floor-map")
    @PreAuthorize(RECEPTIONIST_ROLES)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFloorMap(@PathVariable UUID propertyId) {
        List<Room> rooms = roomRepository.findByPropertyIdAndDeletedFalseOrderByFloorAscRoomNumberAsc(propertyId);
        
        // Group rooms by floor
        Map<Integer, List<RoomDto>> groupedByFloor = rooms.stream()
                .map(this::mapToDto)
                .collect(Collectors.groupingBy(RoomDto::floor));

        List<Map<String, Object>> floorsList = groupedByFloor.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> Map.of(
                        "floor", entry.getKey(),
                        "rooms", entry.getValue()
                ))
                .collect(Collectors.toList());

        Map<String, String> statusColors = Map.of(
                "AVAILABLE", "#22C55E",
                "OCCUPIED", "#EF4444",
                "CLEANING", "#EAB308",
                "MAINTENANCE", "#3B82F6",
                "OUT_OF_ORDER", "#6B7280"
        );

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "floors", floorsList,
                "statusColors", statusColors
        )));
    }

    @Operation(summary = "Tìm phòng trống theo ngày")
    @GetMapping("/api/v1/properties/{propertyId}/rooms/availability")
    @PreAuthorize(RECEPTIONIST_ROLES)
    public ResponseEntity<ApiResponse<List<RoomDto>>> getAvailableRooms(
            @PathVariable UUID propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkOut) {
        
        List<Room> rooms = roomRepository.findAvailableRooms(propertyId, checkIn, checkOut);
        List<RoomDto> dtos = rooms.stream().map(this::mapToDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @Operation(summary = "Thêm phòng mới")
    @PostMapping("/api/v1/properties/{propertyId}/rooms")
    @PreAuthorize(MANAGER_ROLES)
    public ResponseEntity<ApiResponse<RoomDto>> createRoom(
            @PathVariable UUID propertyId,
            @RequestBody CreateRoomRequest request) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property", propertyId.toString()));

        RoomType roomType = roomTypeRepository.findById(request.roomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("RoomType", request.roomTypeId().toString()));

        Room room = new Room();
        room.setProperty(property);
        room.setRoomType(roomType);
        room.setRoomNumber(request.roomNumber());
        room.setFloor(request.floor());
        room.setDescription(request.description());
        room.setStatus(RoomStatus.AVAILABLE);
        room.setInitialWaterIndex(request.initialWaterIndex() != null ? request.initialWaterIndex() : 0.0);
        room.setInitialElectricIndex(request.initialElectricIndex() != null ? request.initialElectricIndex() : 0.0);
        room.setHasBalcony(request.hasBalcony() != null ? request.hasBalcony() : false);
        room.setHasWindow(request.hasWindow() != null ? request.hasWindow() : true);
        room.setViewType(request.viewType() != null ? RoomView.valueOf(request.viewType().toUpperCase()) : RoomView.STREET);

        Room saved = roomRepository.save(room);
        
        // Increase totalRooms count in RoomType
        roomType.setTotalRooms(roomType.getTotalRooms() + 1);
        roomTypeRepository.save(roomType);

        return ResponseEntity.ok(ApiResponse.created(mapToDto(saved)));
    }

    @Operation(summary = "Cập nhật trạng thái phòng")
    @PutMapping("/api/v1/rooms/{id}/status")
    @PreAuthorize(RECEPTIONIST_ROLES)
    public ResponseEntity<ApiResponse<RoomDto>> updateRoomStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", id.toString()));

        room.setStatus(RoomStatus.valueOf(status.toUpperCase()));
        Room saved = roomRepository.save(room);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", mapToDto(saved)));
    }

    @Operation(summary = "Cập nhật dọn phòng")
    @PutMapping("/api/v1/rooms/{id}/housekeeping")
    @PreAuthorize(RECEPTIONIST_ROLES)
    public ResponseEntity<ApiResponse<RoomDto>> updateHousekeeping(
            @PathVariable UUID id,
            @RequestParam String status) {
        return updateRoomStatus(id, status);
    }

    @Operation(summary = "Cập nhật thông tin phòng")
    @PutMapping("/api/v1/rooms/{id}")
    @PreAuthorize(MANAGER_ROLES)
    public ResponseEntity<ApiResponse<RoomDto>> updateRoom(
            @PathVariable UUID id,
            @RequestBody CreateRoomRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", id.toString()));

        RoomType roomType = roomTypeRepository.findById(request.roomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("RoomType", request.roomTypeId().toString()));

        if (!room.getRoomType().getId().equals(roomType.getId())) {
            RoomType oldType = room.getRoomType();
            oldType.setTotalRooms(Math.max(0, oldType.getTotalRooms() - 1));
            roomTypeRepository.save(oldType);
            
            roomType.setTotalRooms(roomType.getTotalRooms() + 1);
            roomTypeRepository.save(roomType);
        }

        room.setRoomType(roomType);
        room.setRoomNumber(request.roomNumber());
        room.setFloor(request.floor());
        room.setDescription(request.description());
        if (request.initialWaterIndex() != null) room.setInitialWaterIndex(request.initialWaterIndex());
        if (request.initialElectricIndex() != null) room.setInitialElectricIndex(request.initialElectricIndex());
        if (request.hasBalcony() != null) room.setHasBalcony(request.hasBalcony());
        if (request.hasWindow() != null) room.setHasWindow(request.hasWindow());
        if (request.viewType() != null) room.setViewType(RoomView.valueOf(request.viewType().toUpperCase()));

        Room saved = roomRepository.save(room);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin phòng thành công", mapToDto(saved)));
    }

    @Operation(summary = "Xóa phòng (Soft Delete)")
    @DeleteMapping("/api/v1/rooms/{id}")
    @PreAuthorize(MANAGER_ROLES)
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable UUID id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", id.toString()));

        room.setDeleted(true);
        roomRepository.save(room);

        RoomType roomType = room.getRoomType();
        roomType.setTotalRooms(Math.max(0, roomType.getTotalRooms() - 1));
        roomTypeRepository.save(roomType);

        return ResponseEntity.ok(ApiResponse.success("Xóa phòng thành công", null));
    }


    // ===== Helpers =====

    private RoomDto mapToDto(Room room) {
        return new RoomDto(
                room.getId(),
                room.getRoomNumber(),
                room.getFloor(),
                room.getDescription(),
                room.getStatus().name(),
                room.getInitialWaterIndex(),
                room.getInitialElectricIndex(),
                room.getHasBalcony(),
                room.getHasWindow(),
                room.getViewType() != null ? room.getViewType().name() : "STREET",
                room.getRoomType().getId(),
                room.getRoomType().getName()
        );
    }

    // ===== Inner Records / DTOs =====

    public record RoomTypeDto(
            UUID id,
            String name,
            String code,
            BigDecimal basePrice,
            Integer maxOccupancy
    ) {}

    public record RoomDto(
            UUID id,
            String roomNumber,
            Integer floor,
            String description,
            String status,
            Double initialWaterIndex,
            Double initialElectricIndex,
            Boolean hasBalcony,
            Boolean hasWindow,
            String viewType,
            UUID roomTypeId,
            String roomTypeName
    ) {}

    public record CreateRoomTypeRequest(
            String name,
            String code,
            BigDecimal basePrice,
            Integer maxOccupancy
    ) {}

    public record CreateRoomRequest(
            String roomNumber,
            Integer floor,
            UUID roomTypeId,
            String description,
            Double initialWaterIndex,
            Double initialElectricIndex,
            Boolean hasBalcony,
            Boolean hasWindow,
            String viewType
    ) {}
}
