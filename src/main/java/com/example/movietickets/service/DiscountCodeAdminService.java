package com.example.movietickets.service;

import com.example.movietickets.dto.request.DiscountCodeRequest;
import com.example.movietickets.entity.DiscountCode;
import com.example.movietickets.exception.ResourceNotFoundException;
import com.example.movietickets.repository.DiscountCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Admin CRUD over discount codes — see DiscountService for the apply-at-booking-time logic. */
@Service
@RequiredArgsConstructor
public class DiscountCodeAdminService {

    private final DiscountCodeRepository discountCodeRepository;

    public List<DiscountCode> getAll() {
        return discountCodeRepository.findAll();
    }

    public DiscountCode create(DiscountCodeRequest request) {
        DiscountCode code = new DiscountCode(null, request.code(), request.type(), request.value(),
            request.minOrderAmount(), request.validFrom(), request.validTo());
        return discountCodeRepository.save(code);
    }

    public DiscountCode update(Long id, DiscountCodeRequest request) {
        DiscountCode code = discountCodeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("DiscountCode " + id + " not found"));
        code.setCode(request.code());
        code.setType(request.type());
        code.setValue(request.value());
        code.setMinOrderAmount(request.minOrderAmount());
        code.setValidFrom(request.validFrom());
        code.setValidTo(request.validTo());
        return discountCodeRepository.save(code);
    }

    public void delete(Long id) {
        if (!discountCodeRepository.existsById(id)) {
            throw new ResourceNotFoundException("DiscountCode " + id + " not found");
        }
        discountCodeRepository.deleteById(id);
    }
}
