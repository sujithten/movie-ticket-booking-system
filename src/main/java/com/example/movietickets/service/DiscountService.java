package com.example.movietickets.service;

import com.example.movietickets.entity.DiscountCode;
import com.example.movietickets.exception.InvalidDiscountCodeException;
import com.example.movietickets.repository.DiscountCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One discount code per booking, FLAT or PERCENTAGE, no stacking (spec §10). */
@Service
@RequiredArgsConstructor
public class DiscountService {

    private final DiscountCodeRepository discountCodeRepository;

    public BigDecimal applyDiscount(String code, BigDecimal orderAmount) {
        if (code == null || code.isBlank()) {
            return orderAmount;
        }

        DiscountCode discount = discountCodeRepository.findByCode(code)
            .orElseThrow(() -> new InvalidDiscountCodeException("Unknown discount code: " + code));

        LocalDateTime now = LocalDateTime.now();
        if (discount.getValidFrom() != null && now.isBefore(discount.getValidFrom())) {
            throw new InvalidDiscountCodeException("Discount code not yet valid: " + code);
        }
        if (discount.getValidTo() != null && now.isAfter(discount.getValidTo())) {
            throw new InvalidDiscountCodeException("Discount code expired: " + code);
        }
        if (discount.getMinOrderAmount() != null && orderAmount.compareTo(discount.getMinOrderAmount()) < 0) {
            throw new InvalidDiscountCodeException(
                "Order amount below minimum " + discount.getMinOrderAmount() + " for code: " + code);
        }

        return switch (discount.getType()) {
            case FLAT -> orderAmount.subtract(discount.getValue()).max(BigDecimal.ZERO);
            case PERCENTAGE -> orderAmount.subtract(
                orderAmount.multiply(discount.getValue()).divide(BigDecimal.valueOf(100)));
        };
    }
}
