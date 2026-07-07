package com.example.movietickets.service;

import com.example.movietickets.dto.request.ShowRequest;
import com.example.movietickets.dto.response.SeatMapEntryResponse;
import com.example.movietickets.entity.MovieShowSeat;
import com.example.movietickets.entity.Screen;
import com.example.movietickets.entity.Seat;
import com.example.movietickets.entity.SeatStatus;
import com.example.movietickets.entity.Show;
import com.example.movietickets.exception.ResourceNotFoundException;
import com.example.movietickets.exception.ScheduleConflictException;
import com.example.movietickets.repository.MovieShowSeatRepository;
import com.example.movietickets.repository.ScreenRepository;
import com.example.movietickets.repository.SeatRepository;
import com.example.movietickets.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShowService {

    private final ShowRepository showRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final MovieShowSeatRepository movieShowSeatRepository;
    private final PricingService pricingService;

    public List<Show> search(Long theaterId, Long movieId, LocalDate date) {
        List<Long> screenIds = theaterId != null
            ? screenRepository.findByTheaterId(theaterId).stream().map(Screen::getId).toList()
            : screenRepository.findAll().stream().map(Screen::getId).toList();

        List<Show> shows = screenIds.isEmpty() ? List.of() : showRepository.findByScreenIdIn(screenIds);

        return shows.stream()
            .filter(s -> movieId == null || s.getMovieId().equals(movieId))
            .filter(s -> date == null || s.getStartTime().toLocalDate().equals(date))
            .toList();
    }

    @Transactional
    public Show createShow(ShowRequest request) {
        List<Show> overlapping = showRepository.findOverlapping(
            request.screenId(), request.startTime(), request.endTime());
        if (!overlapping.isEmpty()) {
            throw new ScheduleConflictException(
                "Screen " + request.screenId() + " already has a show in that time window");
        }

        Show show = showRepository.save(new Show(null, request.movieId(), request.screenId(),
            request.startTime(), request.endTime()));

        List<Seat> seatsForScreen = seatRepository.findByScreenId(request.screenId());
        List<MovieShowSeat> rows = seatsForScreen.stream()
            .map(seat -> new MovieShowSeat(show.getId(), seat.getId(), null, SeatStatus.AVAILABLE, null))
            .toList();
        movieShowSeatRepository.saveAll(rows);

        return show;
    }

    public void deleteShow(Long id) {
        if (!showRepository.existsById(id)) {
            throw new ResourceNotFoundException("Show " + id + " not found");
        }
        showRepository.deleteById(id);
    }

    public List<SeatMapEntryResponse> getSeatMap(Long showId) {
        Show show = showRepository.findById(showId)
            .orElseThrow(() -> new ResourceNotFoundException("Show " + showId + " not found"));

        List<MovieShowSeat> rows = movieShowSeatRepository.findByShowId(showId);
        Map<Long, Seat> seatsById = seatRepository.findAllById(
                rows.stream().map(MovieShowSeat::getSeatId).toList())
            .stream()
            .collect(Collectors.toMap(Seat::getId, s -> s));

        return rows.stream()
            .map(row -> {
                Seat seat = seatsById.get(row.getSeatId());
                return new SeatMapEntryResponse(seat.getId(), seat.getSeatNumber(), seat.getSeatType(),
                    row.getStatus(), pricingService.seatPrice(seat, show));
            })
            .toList();
    }
}
