package com.hotelchain.pro.repository;

import com.hotelchain.pro.entity.User;
import com.hotelchain.pro.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhone(String phone);
    List<User> findByRole(Role role);
    List<User> findByRoleAndIsActiveTrue(Role role);
    boolean existsByPhone(String phone);
}
