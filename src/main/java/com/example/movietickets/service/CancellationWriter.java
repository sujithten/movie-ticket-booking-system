package com.example.movietickets.service;

import com.example.movietickets.entity.*;
import com.example.movietickets.exception.InvalidBookingStateException;
import com.example.movietickets.exception.ResourceNotFoundException;
import com.example.movietickets.repository.BookingRepository;
import com.example.movietickets.repository.BookingSeatRepository;
import com.example.movietickets.repository.MovieShowSeatRepository;
import com.example.movietickets.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Transactional core of cancellation, split out so the caller can notify only after commit. */
@Service
@RequiredArgsConstructor
public class CancellationWriter {

    private final BookingRepository bookingRepository;
    private final ShowRepository showRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final MovieShowSeatRepository movieShowSeatRepository;
    private final RefundService refundService;

    @Transactional
    public CancellationResult cancelBooking(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking " + bookingId + " not found"));

        if (!booking.getUserId().equals(userId)) {
            throw new AccessDeniedException("Booking " + bookingId + " does not belong to this user");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingStateException("Booking " + bookingId + " is already cancelled");
        }

        Show show = showRepository.findById(booking.getShowId())
            .orElseThrow(() -> new ResourceNotFoundException("Show " + booking.getShowId() + " not found"));

        LocalDateTime now = LocalDateTime.now();
        if (!refundService.isCancellationAllowed(show.getStartTime(), now)) {
            throw new InvalidBookingStateException(
                "Booking " + bookingId + " can no longer be cancelled — past the cancellation cutoff");
        }

        BigDecimal refundAmount = refundService.calculateRefund(booking.getTotalAmount(), show.getStartTime(), now);

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        List<Long> seatIds = bookingSeatRepository.findByBookingId(bookingId).stream()
            .map(BookingSeat::getId).map(id -> id.getSeatId())
            .sorted()
            .toList();
        for (Long seatId : seatIds) {
            movieShowSeatRepository.findByShowIdAndSeatIdForUpdate(show.getId(), seatId).ifPresent(row -> {
                row.setStatus(SeatStatus.AVAILABLE);
                row.setUserId(null);
                row.setExpiresAt(null);
                movieShowSeatRepository.save(row);
            });
        }

        return new CancellationResult(booking, refundAmount);
    }
}
