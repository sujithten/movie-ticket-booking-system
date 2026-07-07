package com.example.movietickets.service;

import com.example.movietickets.dto.request.RefundPolicyRequest;
import com.example.movietickets.entity.RefundPolicy;
import com.example.movietickets.exception.ResourceNotFoundException;
import com.example.movietickets.repository.RefundPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Admin CRUD over refund policies — see RefundService for the refund-calculation logic. */
@Service
@RequiredArgsConstructor
public class RefundPolicyAdminService {

    private final RefundPolicyRepository refundPolicyRepository;

    public List<RefundPolicy> getAll() {
        return refundPolicyRepository.findAll();
    }

    public RefundPolicy create(RefundPolicyRequest request) {
        return refundPolicyRepository.save(
            new RefundPolicy(null, request.cutoffHoursBeforeShow(), request.refundPercentage()));
    }

    public RefundPolicy update(Long id, RefundPolicyRequest request) {
        RefundPolicy policy = refundPolicyRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("RefundPolicy " + id + " not found"));
        policy.setCutoffHoursBeforeShow(request.cutoffHoursBeforeShow());
        policy.setRefundPercentage(request.refundPercentage());
        return refundPolicyRepository.save(policy);
    }

    public void delete(Long id) {
        if (!refundPolicyRepository.existsById(id)) {
            throw new ResourceNotFoundException("RefundPolicy " + id + " not found");
        }
        refundPolicyRepository.deleteById(id);
    }
}
