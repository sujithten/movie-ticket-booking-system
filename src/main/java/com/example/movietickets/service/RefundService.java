package com.example.movietickets.service;

import com.example.movietickets.config.BookingProperties;
import com.example.movietickets.entity.RefundPolicy;
import com.example.movietickets.repository.RefundPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/** Refund computed via RefundPolicy.refundPercentage based on cutoffHoursBeforeShow (spec §9). */
@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundPolicyRepository refundPolicyRepository;
    private final BookingProperties bookingProperties;

    public boolean isCancellationAllowed(LocalDateTime showStartTime, LocalDateTime now) {
        long hoursUntilShow = Duration.between(now, showStartTime).toHours();
        return hoursUntilShow >= bookingProperties.cancellationMinHoursBeforeShow();
    }

    public BigDecimal calculateRefund(BigDecimal totalAmount, LocalDateTime showStartTime, LocalDateTime now) {
        long hoursUntilShow = Duration.between(now, showStartTime).toHours();

        int refundPercentage = refundPolicyRepository.findAllByOrderByCutoffHoursBeforeShowDesc().stream()
            .filter(policy -> hoursUntilShow >= policy.getCutoffHoursBeforeShow())
            .findFirst()
            .map(RefundPolicy::getRefundPercentage)
            .orElse(0);

        return totalAmount.multiply(BigDecimal.valueOf(refundPercentage))
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
