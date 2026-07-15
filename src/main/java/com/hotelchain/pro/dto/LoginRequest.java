package com.hotelchain.pro.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    private String phone;
}
