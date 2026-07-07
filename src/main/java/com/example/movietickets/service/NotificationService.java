package com.example.movietickets.service;

import com.example.movietickets.entity.Booking;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Mocked/logged notifications (spec §12) — no real message broker. Fired
 * @Async so booking confirmation/cancellation never blocks the HTTP response.
 */
@Service
@Slf4j
public class NotificationService {

    @Async("notificationExecutor")
    public void sendBookingConfirmation(Booking booking) {
        log.info("[MOCK NOTIFICATION] Booking {} confirmed for user {} — amount {}",
            booking.getId(), booking.getUserId(), booking.getTotalAmount());
    }

    @Async("notificationExecutor")
    public void sendBookingCancellation(Booking booking, java.math.BigDecimal refundAmount) {
        log.info("[MOCK NOTIFICATION] Booking {} cancelled for user {} — refund {}",
            booking.getId(), booking.getUserId(), refundAmount);
    }

    @Async("notificationExecutor")
    public void sendShowReminder(Booking booking) {
        log.info("[MOCK NOTIFICATION] Reminder: booking {} for user {} has an upcoming show",
            booking.getId(), booking.getUserId());
    }
}
