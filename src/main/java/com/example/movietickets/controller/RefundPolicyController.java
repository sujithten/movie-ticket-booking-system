package com.example.movietickets.controller;

import com.example.movietickets.dto.request.RefundPolicyRequest;
import com.example.movietickets.entity.RefundPolicy;
import com.example.movietickets.service.RefundPolicyAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/refund-policies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RefundPolicyController {

    private final RefundPolicyAdminService refundPolicyAdminService;

    @GetMapping
    public List<RefundPolicy> getAll() {
        return refundPolicyAdminService.getAll();
    }

    @PostMapping
    public RefundPolicy create(@Valid @RequestBody RefundPolicyRequest request) {
        return refundPolicyAdminService.create(request);
    }

    @PutMapping("/{id}")
    public RefundPolicy update(@PathVariable Long id, @Valid @RequestBody RefundPolicyRequest request) {
        return refundPolicyAdminService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        refundPolicyAdminService.delete(id);
    }
}
