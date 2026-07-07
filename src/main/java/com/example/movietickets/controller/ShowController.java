package com.example.movietickets.controller;

import com.example.movietickets.dto.request.LockSeatsRequest;
import com.example.movietickets.dto.request.ShowRequest;
import com.example.movietickets.dto.response.LockSeatsResponse;
import com.example.movietickets.dto.response.SeatMapEntryResponse;
import com.example.movietickets.entity.MovieShowSeat;
import com.example.movietickets.entity.Show;
import com.example.movietickets.security.CurrentUser;
import com.example.movietickets.service.SeatLockService;
import com.example.movietickets.service.ShowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;
    private final SeatLockService seatLockService;

    @GetMapping
    public List<Show> search(@RequestParam(required = false) Long theaterId,
                              @RequestParam(required = false) Long movieId,
                              @RequestParam(required = false) LocalDate date) {
        return showService.search(theaterId, movieId, date);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Show create(@Valid @RequestBody ShowRequest request) {
        return showService.createShow(request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        showService.deleteShow(id);
    }

    @GetMapping("/{id}/seats")
    public List<SeatMapEntryResponse> seatMap(@PathVariable Long id) {
        return showService.getSeatMap(id);
    }

    @PostMapping("/{id}/seats/lock")
    public LockSeatsResponse lockSeats(@PathVariable Long id, @Valid @RequestBody LockSeatsRequest request) {
        Long userId = CurrentUser.id();
        List<MovieShowSeat> locked = seatLockService.lockSeats(id, request.seatIds(), userId);
        return new LockSeatsResponse(
            locked.stream().map(MovieShowSeat::getSeatId).toList(),
            locked.get(0).getExpiresAt());
    }
}
