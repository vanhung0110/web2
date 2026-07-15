package com.hotelchain.pro.controller;

import com.hotelchain.pro.dto.ApiResponse;
import com.hotelchain.pro.dto.CreateRoomRequest;
import com.hotelchain.pro.entity.Room;
import com.hotelchain.pro.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Room>>> getAllRooms() {
        return ResponseEntity.ok(ApiResponse.ok(roomService.getAllRooms()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Room>> getRoom(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(roomService.getRoom(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Room>> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Tạo phòng thành công", roomService.createRoom(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Room>> updateRoom(@PathVariable UUID id, @Valid @RequestBody CreateRoomRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật phòng thành công", roomService.updateRoom(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable UUID id) {
        roomService.deleteRoom(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa phòng thành công"));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStats() {
        Map<String, Long> stats = Map.of(
                "total", (long) roomService.getAllRooms().size(),
                "occupied", roomService.countOccupied(),
                "available", roomService.countAvailable()
        );
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
