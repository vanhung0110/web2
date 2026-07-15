package com.hotelchain.pro.service;

import com.hotelchain.pro.dto.BranchDto;
import com.hotelchain.pro.entity.Branch;
import com.hotelchain.pro.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;

    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    public Branch getBranch(UUID id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cơ sở không tồn tại"));
    }

    @Transactional
    public Branch createBranch(BranchDto request) {
        Branch branch = new Branch();
        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        return branchRepository.save(branch);
    }

    @Transactional
    public Branch updateBranch(UUID id, BranchDto request) {
        Branch branch = getBranch(id);
        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        return branchRepository.save(branch);
    }

    @Transactional
    public void deleteBranch(UUID id) {
        Branch branch = getBranch(id);
        branchRepository.delete(branch);
    }
}
