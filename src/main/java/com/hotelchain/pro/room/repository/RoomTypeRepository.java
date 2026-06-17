package com.hotelchain.pro.room.repository;

import com.hotelchain.pro.room.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, UUID> {

    List<RoomType> findByPropertyIdAndIsActiveTrue(UUID propertyId);

    boolean existsByPropertyIdAndCodeAndDeletedFalse(UUID propertyId, String code);
}
