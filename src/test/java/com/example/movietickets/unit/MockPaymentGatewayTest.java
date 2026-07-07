package com.example.movietickets.unit;

import com.example.movietickets.payment.MockPaymentGateway;
import com.example.movietickets.payment.PaymentResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MockPaymentGatewayTest {

    private final MockPaymentGateway gateway = new MockPaymentGateway();

    @Test
    void succeedsWhenNotAskedToSimulateFailure() {
        PaymentResult result = gateway.charge(BigDecimal.valueOf(100), 1L, false);

        assertThat(result.success()).isTrue();
        assertThat(result.reference()).isNotBlank();
    }

    @Test
    void declinesDeterministicallyWhenAskedTo() {
        PaymentResult result = gateway.charge(BigDecimal.valueOf(100), 1L, true);

        assertThat(result.success()).isFalse();
        assertThat(result.reference()).isNull();
    }
}
