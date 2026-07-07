package com.example.movietickets.integration;

import com.example.movietickets.dto.request.CreateBookingRequest;
import com.example.movietickets.entity.*;
import com.example.movietickets.repository.*;
import com.example.movietickets.service.BookingService;
import com.example.movietickets.service.CancellationResult;
import com.example.movietickets.service.SeatLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CancellationRefundIT extends AbstractIntegrationTest {

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
    private RefundPolicyRepository refundPolicyRepository;

    private static final Long USER_ID = 200L;
    private Long showId;
    private Long seatId;

    @BeforeEach
    void setUp() {
        refundPolicyRepository.save(new RefundPolicy(null, 48, 100));
        refundPolicyRepository.save(new RefundPolicy(null, 24, 50));

        City city = cityRepository.save(new City(null, "Test City"));
        Theater theater = theaterRepository.save(new Theater(null, "Test Theater", city.getId(), "addr"));
        Screen screen = screenRepository.save(new Screen(null, theater.getId(), "Screen 1", 50));
        Seat seat = seatRepository.save(new Seat(null, screen.getId(), "A1", SeatType.REGULAR));
        Movie movie = movieRepository.save(new Movie(null, "Test Movie", 120, "en", "Action"));
        // far enough out that the cancellation-cutoff and the 100% refund tier both apply
        Show show = showRepository.save(new Show(null, movie.getId(), screen.getId(),
            LocalDateTime.now().plusDays(5), LocalDateTime.now().plusDays(5).plusHours(2)));
        movieShowSeatRepository.save(new MovieShowSeat(show.getId(), seat.getId(), null, SeatStatus.AVAILABLE, null));

        this.showId = show.getId();
        this.seatId = seat.getId();
    }

    @Test
    void cancellingConfirmedBookingRefundsAndReleasesSeat() {
        seatLockService.lockSeat(showId, seatId, USER_ID);
        Booking booking = bookingService.confirmBooking(USER_ID,
            new CreateBookingRequest(showId, List.of(seatId), null, false), null);

        CancellationResult result = bookingService.cancelBooking(USER_ID, booking.getId());

        assertThat(result.booking().getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(result.refundAmount()).isEqualByComparingTo(booking.getTotalAmount());

        MovieShowSeat row = movieShowSeatRepository.findByShowId(showId).get(0);
        assertThat(row.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(row.getUserId()).isNull();
    }

    @Test
    void cancellingAlreadyCancelledBookingRejected() {
        seatLockService.lockSeat(showId, seatId, USER_ID);
        Booking booking = bookingService.confirmBooking(USER_ID,
            new CreateBookingRequest(showId, List.of(seatId), null, false), null);
        bookingService.cancelBooking(USER_ID, booking.getId());

        org.junit.jupiter.api.Assertions.assertThrows(
            com.example.movietickets.exception.InvalidBookingStateException.class,
            () -> bookingService.cancelBooking(USER_ID, booking.getId()));
    }
}
