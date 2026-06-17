package com.hotelchain.pro.auth.repository;

import com.hotelchain.pro.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :userId")
    void incrementFailedLoginAttempts(UUID userId);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.accountLocked = false, u.lockedUntil = null WHERE u.id = :userId")
    void resetLoginAttempts(UUID userId);
}
