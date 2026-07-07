package com.example.movietickets.controller;

import com.example.movietickets.dto.request.SeatRequest;
import com.example.movietickets.entity.Seat;
import com.example.movietickets.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping
    public List<Seat> getAll(@RequestParam(required = false) Long screenId) {
        return seatService.getAll(screenId);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Seat create(@Valid @RequestBody SeatRequest request) {
        return seatService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Seat update(@PathVariable Long id, @Valid @RequestBody SeatRequest request) {
        return seatService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        seatService.delete(id);
    }
}
