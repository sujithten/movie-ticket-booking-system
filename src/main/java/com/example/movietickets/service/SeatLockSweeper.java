package com.example.movietickets.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hygiene only, not correctness-critical (spec §5) — keeps read-only seat-map
 * queries accurate without every read needing an expiresAt check. Actual lock
 * acquisition already reclaims expired locks inline in SeatLockService.
 */
@Component
@RequiredArgsConstructor
public class SeatLockSweeper {

    private final SeatLockService seatLockService;

    @Scheduled(fixedRate = 30000)
    public void releaseExpiredLocks() {
        seatLockService.releaseExpiredLocks();
    }
}
