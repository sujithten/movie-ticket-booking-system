package com.example.movietickets.integration;

import com.example.movietickets.dto.request.CreateBookingRequest;
import com.example.movietickets.entity.*;
import com.example.movietickets.exception.PaymentFailedException;
import com.example.movietickets.repository.*;
import com.example.movietickets.service.BookingService;
import com.example.movietickets.service.SeatLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingConfirmationIT extends AbstractIntegrationTest {

    @Autowired
    private SeatLockService seatLockService;
    @Autowired
    private BookingService bookingService;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private TheaterRepository theaterRepository;
    @Autowired
    private ScreenRepository screenRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private ShowRepository showRepository;
    @Autowired
    private MovieShowSeatRepository movieShowSeatRepository;
    @Autowired
    private BookingRepository bookingRepository;

    private Long showId;
    private Long seatId;
    private static final Long USER_ID = 100L;

    @BeforeEach
    void setUp() {
        City city = cityRepository.save(new City(null, "Test City"));
        Theater theater = theaterRepository.save(new Theater(null, "Test Theater", city.getId(), "addr"));
        Screen screen = screenRepository.save(new Screen(null, theater.getId(), "Screen 1", 50));
        Seat seat = seatRepository.save(new Seat(null, screen.getId(), "A1", SeatType.REGULAR));
        Movie movie = movieRepository.save(new Movie(null, "Test Movie", 120, "en", "Action"));
        // 2024-01-01 is a Monday — weekday, deterministic price
        Show show = showRepository.save(new Show(null, movie.getId(), screen.getId(),
            LocalDateTime.of(2024, 1, 1, 10, 0), LocalDateTime.of(2024, 1, 1, 12, 0)));
        movieShowSeatRepository.save(new MovieShowSeat(show.getId(), seat.getId(), null, SeatStatus.AVAILABLE, null));

        this.showId = show.getId();
        this.seatId = seat.getId();
    }

    @Test
    void successfulPaymentConfirmsSeatsAndBooking() {
        seatLockService.lockSeat(showId, seatId, USER_ID);

        Booking booking = bookingService.confirmBooking(USER_ID,
            new CreateBookingRequest(showId, List.of(seatId), null, false), null);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        MovieShowSeat row = movieShowSeatRepository.findByShowId(showId).get(0);
        assertThat(row.getStatus()).isEqualTo(SeatStatus.CONFIRMED);
    }

    @Test
    void failedPaymentLeavesSeatLockedAndCreatesNoBooking() {
        seatLockService.lockSeat(showId, seatId, USER_ID);

        assertThatThrownBy(() -> bookingService.confirmBooking(USER_ID,
            new CreateBookingRequest(showId, List.of(seatId), null, true), null))
            .isInstanceOf(PaymentFailedException.class);

        MovieShowSeat row = movieShowSeatRepository.findByShowId(showId).get(0);
        assertThat(row.getStatus()).isEqualTo(SeatStatus.LOCKED);
        assertThat(row.getUserId()).isEqualTo(USER_ID);
        assertThat(bookingRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).isEmpty();

        // seat is still held — a retry before expiry succeeds without re-locking
        Booking booking = bookingService.confirmBooking(USER_ID,
            new CreateBookingRequest(showId, List.of(seatId), null, false), null);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void sameIdempotencyKeySubmittedTwiceCreatesOnlyOneBooking() {
        seatLockService.lockSeat(showId, seatId, USER_ID);
        String key = UUID.randomUUID().toString();

        Booking first = bookingService.confirmBooking(USER_ID,
            new CreateBookingRequest(showId, List.of(seatId), null, false), key);
        Booking second = bookingService.confirmBooking(USER_ID,
            new CreateBookingRequest(showId, List.of(seatId), null, false), key);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(bookingRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).hasSize(1);
    }
}
