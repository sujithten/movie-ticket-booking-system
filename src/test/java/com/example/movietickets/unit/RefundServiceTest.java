package com.example.movietickets.unit;

import com.example.movietickets.config.BookingProperties;
import com.example.movietickets.entity.RefundPolicy;
import com.example.movietickets.repository.RefundPolicyRepository;
import com.example.movietickets.service.RefundService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private RefundPolicyRepository refundPolicyRepository;

    // >=48h -> 100%, >=24h -> 50%, below 24h -> 0% (via the empty-match fallback)
    private final List<RefundPolicy> tiers = List.of(
        new RefundPolicy(1L, 48, 100),
        new RefundPolicy(2L, 24, 50)
    );

    private RefundService newService(int cancellationMinHours) {
        return new RefundService(refundPolicyRepository,
            new BookingProperties(10, cancellationMinHours, 24));
    }

    @Test
    void fullRefundWhenWellBeforeCutoff() {
        lenient().when(refundPolicyRepository.findAllByOrderByCutoffHoursBeforeShowDesc()).thenReturn(tiers);
        RefundService service = newService(1);
        LocalDateTime now = LocalDateTime.now();

        BigDecimal refund = service.calculateRefund(BigDecimal.valueOf(1000), now.plusHours(72), now);

        assertThat(refund).isEqualByComparingTo("1000.00");
    }

    @Test
    void exactlyAtBoundaryUsesThatTier() {
        lenient().when(refundPolicyRepository.findAllByOrderByCutoffHoursBeforeShowDesc()).thenReturn(tiers);
        RefundService service = newService(1);
        LocalDateTime now = LocalDateTime.now();

        BigDecimal refund = service.calculateRefund(BigDecimal.valueOf(1000), now.plusHours(24), now);

        assertThat(refund).isEqualByComparingTo("500.00");
    }

    @Test
    void belowSmallestTierYieldsZeroRefundButStillAllowedIfAboveMinCutoff() {
        lenient().when(refundPolicyRepository.findAllByOrderByCutoffHoursBeforeShowDesc()).thenReturn(tiers);
        RefundService service = newService(1);
        LocalDateTime now = LocalDateTime.now();

        BigDecimal refund = service.calculateRefund(BigDecimal.valueOf(1000), now.plusHours(2), now);

        assertThat(refund).isEqualByComparingTo("0.00");
        assertThat(service.isCancellationAllowed(now.plusHours(2), now)).isTrue();
    }

    @Test
    void cancellationRejectedPastMinCutoff() {
        RefundService service = newService(1);
        LocalDateTime now = LocalDateTime.now();

        assertThat(service.isCancellationAllowed(now.plusMinutes(30), now)).isFalse();
    }
}
