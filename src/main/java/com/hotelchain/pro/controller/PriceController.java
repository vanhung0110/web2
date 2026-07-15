package com.hotelchain.pro.controller;

import com.hotelchain.pro.dto.ApiResponse;
import com.hotelchain.pro.entity.UtilityPrice;
import com.hotelchain.pro.repository.UtilityPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class PriceController {

    private final UtilityPriceRepository priceRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<UtilityPrice>> getCurrentPrices() {
        UtilityPrice price = priceRepository.findFirstByOrderByEffectiveFromDesc()
                .orElseThrow(() -> new RuntimeException("Chưa cấu hình đơn giá"));
        return ResponseEntity.ok(ApiResponse.ok(price));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UtilityPrice>> updatePrices(@RequestBody UtilityPrice request) {
        UtilityPrice price = new UtilityPrice();
        price.setWaterPricePerUnit(request.getWaterPricePerUnit());
        price.setElectricPricePerUnit(request.getElectricPricePerUnit());
        price.setEffectiveFrom(request.getEffectiveFrom() != null ? request.getEffectiveFrom() : java.time.LocalDate.now());
        price.setInternetFee(request.getInternetFee());
        price.setTrashFee(request.getTrashFee());
        price.setBankName(request.getBankName());
        price.setBankAccount(request.getBankAccount());
        price.setAccountName(request.getAccountName());
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật đơn giá thành công", priceRepository.save(price)));
    }
}
