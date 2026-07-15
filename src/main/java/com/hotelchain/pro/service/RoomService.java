package com.hotelchain.pro.service;

import com.hotelchain.pro.dto.CreateRoomRequest;
import com.hotelchain.pro.entity.Room;
import com.hotelchain.pro.entity.Branch;
import com.hotelchain.pro.repository.RoomRepository;
import com.hotelchain.pro.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final BranchRepository branchRepository;

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public Room getRoom(UUID id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Phòng không tồn tại"));
    }

    @Transactional
    public Room createRoom(CreateRoomRequest request) {
        if (roomRepository.existsByRoomNumber(request.getRoomNumber())) {
            throw new RuntimeException("Số phòng đã tồn tại: " + request.getRoomNumber());
        }

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Chi nhánh không tồn tại"));

        Room room = new Room();
        room.setRoomNumber(request.getRoomNumber());
        room.setFloor(request.getFloor() != null ? request.getFloor() : 1);
        room.setMonthlyRent(request.getMonthlyRent());
        room.setDailyRent(request.getDailyRent() != null ? request.getDailyRent() : java.math.BigDecimal.ZERO);
        room.setDescription(request.getDescription());
        room.setBranch(branch);
        room.setIsOccupied(false);
        return roomRepository.save(room);
    }

    @Transactional
    public Room updateRoom(UUID id, CreateRoomRequest request) {
        Room room = getRoom(id);
        
        if (request.getBranchId() != null) {
            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Chi nhánh không tồn tại"));
            room.setBranch(branch);
        }
        
        room.setRoomNumber(request.getRoomNumber());
        room.setFloor(request.getFloor() != null ? request.getFloor() : room.getFloor());
        room.setMonthlyRent(request.getMonthlyRent());
        if (request.getDailyRent() != null) {
            room.setDailyRent(request.getDailyRent());
        }
        room.setDescription(request.getDescription());
        return roomRepository.save(room);
    }

    @Transactional
    public void deleteRoom(UUID id) {
        Room room = getRoom(id);
        if (room.getIsOccupied()) {
            throw new RuntimeException("Không thể xóa phòng đang có người thuê");
        }
        roomRepository.delete(room);
    }

    public long countOccupied() {
        return roomRepository.countByIsOccupiedTrue();
    }

    public long countAvailable() {
        return roomRepository.countByIsOccupiedFalse();
    }
}
