package com.hotelchain.pro.controller;

import com.hotelchain.pro.dto.ApiResponse;
import com.hotelchain.pro.dto.BranchDto;
import com.hotelchain.pro.entity.Branch;
import com.hotelchain.pro.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Branch>>> getAllBranches() {
        return ResponseEntity.ok(ApiResponse.ok(branchService.getAllBranches()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Branch>> createBranch(@RequestBody BranchDto request) {
        return ResponseEntity.ok(ApiResponse.ok("Tạo cơ sở thành công", branchService.createBranch(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Branch>> updateBranch(@PathVariable UUID id, @RequestBody BranchDto request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật cơ sở thành công", branchService.updateBranch(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(@PathVariable UUID id) {
        branchService.deleteBranch(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa cơ sở thành công"));
    }
}
