package com.example.movietickets.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * No real payment gateway integration (explicitly out of scope, spec §11).
 * Failure is deterministic — driven by the caller-supplied simulateFailure flag —
 * never random, so tests stay reproducible.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public PaymentResult charge(BigDecimal amount, Long userId, boolean simulateFailure) {
        if (simulateFailure) {
            return PaymentResult.declined();
        }
        return PaymentResult.success(UUID.randomUUID().toString());
    }
}
