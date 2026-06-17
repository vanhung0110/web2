package com.hotelchain.pro.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception cho toàn hệ thống.
 */
@Getter
public class HotelChainException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public HotelChainException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public HotelChainException(String errorCode, String message) {
        this(errorCode, message, HttpStatus.BAD_REQUEST);
    }
}
