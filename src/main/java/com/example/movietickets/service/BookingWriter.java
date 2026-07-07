package com.example.movietickets.service;

import com.example.movietickets.dto.request.CreateBookingRequest;
import com.example.movietickets.entity.*;
import com.example.movietickets.exception.PaymentFailedException;
import com.example.movietickets.exception.ResourceNotFoundException;
import com.example.movietickets.exception.SeatUnavailableException;
import com.example.movietickets.payment.PaymentGateway;
import com.example.movietickets.payment.PaymentResult;
import com.example.movietickets.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The transactional core of booking confirmation, isolated in its own bean so
 * BookingService (the outer orchestrator) can catch a unique-constraint
 * violation on the idempotency key from *outside* this transaction — catching
 * it inside would leave the JPA persistence context contaminated after a failed flush.
 */
@Service
@RequiredArgsConstructor
public class BookingWriter {

    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final MovieShowSeatRepository movieShowSeatRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final PricingService pricingService;
    private final DiscountService discountService;
    private final PaymentGateway paymentGateway;

    @Transactional
    public Booking confirmBooking(Long userId, CreateBookingRequest request, String idempotencyKey) {
        Show show = showRepository.findById(request.showId())
            .orElseThrow(() -> new ResourceNotFoundException("Show " + request.showId() + " not found"));

        List<Long> orderedSeatIds = request.seatIds().stream().sorted().toList();

        // Re-validate every seat is LOCKED, unexpired, and owned by this user — same
        // sorted-lock-order rule as SeatLockService to avoid deadlocking against a
        // concurrent lock/confirm call touching an overlapping seat set.
        List<MovieShowSeat> seatRows = new ArrayList<>();
        for (Long seatId : orderedSeatIds) {
            MovieShowSeat row = movieShowSeatRepository.findByShowIdAndSeatIdForUpdate(request.showId(), seatId)
                .orElseThrow(() -> new SeatUnavailableException(seatId));
            boolean ownedAndLocked = row.getStatus() == SeatStatus.LOCKED
                && userId.equals(row.getUserId())
                && row.getExpiresAt() != null && row.getExpiresAt().isAfter(Instant.now());
            if (!ownedAndLocked) {
                throw new SeatUnavailableException(seatId);
            }
            seatRows.add(row);
        }

        Map<Long, Seat> seatsById = seatRepository.findAllById(orderedSeatIds).stream()
            .collect(Collectors.toMap(Seat::getId, s -> s));

        BigDecimal subtotal = orderedSeatIds.stream()
            .map(seatId -> pricingService.seatPrice(seatsById.get(seatId), show))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = discountService.applyDiscount(request.discountCode(), subtotal);

        PaymentResult result = paymentGateway.charge(total, userId, request.simulatePaymentFailure());
        if (!result.success()) {
            // Rolls back here — seats are never touched, so they remain LOCKED under
            // this user for whatever's left of the hold window. No Booking row exists.
            throw new PaymentFailedException("Payment declined for user " + userId);
        }

        seatRows.forEach(row -> row.setStatus(SeatStatus.CONFIRMED));
        movieShowSeatRepository.saveAll(seatRows);

        Long discountCodeId = null;
        if (request.discountCode() != null && !request.discountCode().isBlank()) {
            discountCodeId = discountCodeRepository.findByCode(request.discountCode())
                .map(DiscountCode::getId).orElse(null);
        }

        Booking savedBooking = bookingRepository.save(new Booking(null, userId, show.getId(),
            BookingStatus.CONFIRMED, total, discountCodeId, idempotencyKey, null));

        List<BookingSeat> bookingSeats = orderedSeatIds.stream()
            .map(seatId -> new BookingSeat(savedBooking.getId(), seatId))
            .toList();
        bookingSeatRepository.saveAll(bookingSeats);

        return savedBooking;
    }
}
