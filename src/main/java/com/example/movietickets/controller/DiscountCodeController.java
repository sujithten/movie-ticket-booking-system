package com.example.movietickets.controller;

import com.example.movietickets.dto.request.DiscountCodeRequest;
import com.example.movietickets.entity.DiscountCode;
import com.example.movietickets.service.DiscountCodeAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/discount-codes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DiscountCodeController {

    private final DiscountCodeAdminService discountCodeAdminService;

    @GetMapping
    public List<DiscountCode> getAll() {
        return discountCodeAdminService.getAll();
    }

    @PostMapping
    public DiscountCode create(@Valid @RequestBody DiscountCodeRequest request) {
        return discountCodeAdminService.create(request);
    }

    @PutMapping("/{id}")
    public DiscountCode update(@PathVariable Long id, @Valid @RequestBody DiscountCodeRequest request) {
        return discountCodeAdminService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        discountCodeAdminService.delete(id);
    }
}
