package com.hotelchain.pro.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends HotelChainException {
    public ResourceNotFoundException(String resource, String id) {
        super("NOT_FOUND", String.format("%s với id '%s' không tồn tại", resource, id), HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String message) {
        super("NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
}
