package com.hotelchain.pro.common.enums;

/**
 * Vai trò người dùng trong hệ thống RBAC.
 * Cấp bậc: SUPER_ADMIN > TENANT_OWNER > CHAIN_MANAGER > PROPERTY_MANAGER > RECEPTIONIST/ACCOUNTANT/HOUSEKEEPING/MAINTENANCE
 */
public enum Role {
    SUPER_ADMIN,        // Quản trị toàn hệ thống
    TENANT_OWNER,       // Chủ doanh nghiệp
    CHAIN_MANAGER,      // Quản lý chuỗi (toàn bộ chi nhánh)
    PROPERTY_MANAGER,   // Quản lý cơ sở (1 chi nhánh)
    RECEPTIONIST,       // Lễ tân
    ACCOUNTANT,         // Kế toán
    HOUSEKEEPING,       // Buồng phòng
    MAINTENANCE         // Kỹ thuật
}
