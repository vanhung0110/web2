package com.hotelchain.pro.controller;

import com.hotelchain.pro.dto.ApiResponse;
import com.hotelchain.pro.dto.CreateTenantRequest;
import com.hotelchain.pro.entity.Tenant;
import com.hotelchain.pro.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Tenant>>> getAllTenants() {
        return ResponseEntity.ok(ApiResponse.ok(tenantService.getAllActiveTenants()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Tenant>> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Tạo người thuê thành công", tenantService.createTenant(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> removeTenant(@PathVariable UUID id) {
        tenantService.removeTenant(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã trả phòng thành công"));
    }
}
