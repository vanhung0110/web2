package com.hotelchain.pro.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class BranchDto {
    private UUID id;
    private String name;
    private String address;
}
