package com.hotelchain.pro.common.exception;

import org.springframework.http.HttpStatus;

/** AUTH_001 — AUTH_006 */
public class AuthException extends HotelChainException {
    public AuthException(String code, String message) {
        super(code, message, HttpStatus.UNAUTHORIZED);
    }

    public static AuthException invalidCredentials() {
        return new AuthException("AUTH_001", "Sai tên đăng nhập hoặc mật khẩu");
    }

    public static AuthException tokenExpired() {
        return new AuthException("AUTH_002", "Token hết hạn");
    }

    public static AuthException tokenInvalid() {
        return new AuthException("AUTH_003", "Token không hợp lệ");
    }

    public static AuthException accountLocked() {
        return new AuthException("AUTH_004", "Tài khoản bị khóa");
    }

    public static AuthException accessDenied() {
        return new AuthException("AUTH_005", "Không đủ quyền truy cập") {
            @Override
            public org.springframework.http.HttpStatus getHttpStatus() {
                return HttpStatus.FORBIDDEN;
            }
        };
    }

    public static AuthException twoFactorRequired() {
        return new AuthException("AUTH_006", "Cần xác thực 2 bước");
    }
}
