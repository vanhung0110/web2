package com.hotelchain.pro.loyalty.entity;

import com.hotelchain.pro.booking.entity.Guest;
import com.hotelchain.pro.common.entity.BaseEntity;
import com.hotelchain.pro.common.enums.MembershipTier;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * LoyaltyAccount — Chương trình tích điểm khách hàng.
 * Earn rate: 1,000đ = 1 điểm
 * Redeem rate: 1 điểm = 500đ
 * Tiers: Bronze(0), Silver(500pts), Gold(2000pts), Platinum(10000pts)
 */
@Entity
@Table(name = "loyalty_accounts", indexes = {
        @Index(name = "idx_loyalty_guest", columnList = "guest_id", unique = true)
})
@Getter
@Setter
public class LoyaltyAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false, unique = true)
    private Guest guest;

    @Column(nullable = false)
    private Long totalPoints = 0L;          // Tổng điểm tích lũy

    @Column(nullable = false)
    private Long availablePoints = 0L;      // Điểm có thể dùng

    @Column(nullable = false)
    private Long usedPoints = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipTier tier = MembershipTier.BRONZE;

    private LocalDateTime tierUpgradedAt;

    // Earn rate: 1,000đ spent = 1 point
    private static final long EARN_RATE = 1000L;    // đồng / điểm
    // Redeem rate: 1 point = 500đ discount
    private static final long REDEEM_RATE = 500L;   // đồng / điểm

    /**
     * Tính toán tier dựa trên tổng điểm.
     */
    public MembershipTier calculateTier() {
        if (totalPoints >= 10000) return MembershipTier.PLATINUM;
        if (totalPoints >= 2000) return MembershipTier.GOLD;
        if (totalPoints >= 500) return MembershipTier.SILVER;
        return MembershipTier.BRONZE;
    }

    /**
     * Tính điểm tích lũy từ số tiền thanh toán.
     */
    public long calculateEarnedPoints(long amountPaid) {
        return amountPaid / EARN_RATE;
    }

    /**
     * Tính số tiền giảm giá từ điểm.
     */
    public long calculateDiscount(long pointsToUse) {
        return Math.min(pointsToUse, availablePoints) * REDEEM_RATE;
    }
}
