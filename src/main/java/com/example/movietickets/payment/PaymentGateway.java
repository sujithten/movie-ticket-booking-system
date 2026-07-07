package com.example.movietickets.payment;

import java.math.BigDecimal;

public interface PaymentGateway {

    PaymentResult charge(BigDecimal amount, Long userId, boolean simulateFailure);
}
