package com.example.movietickets.controller;

import com.example.movietickets.dto.request.SeatTypePriceRequest;
import com.example.movietickets.entity.SeatTypePrice;
import com.example.movietickets.service.SeatTypePriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seat-type-prices")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SeatTypePriceController {

    private final SeatTypePriceService seatTypePriceService;

    @GetMapping
    public List<SeatTypePrice> getAll() {
        return seatTypePriceService.getAll();
    }

    @PostMapping
    public SeatTypePrice create(@Valid @RequestBody SeatTypePriceRequest request) {
        return seatTypePriceService.create(request);
    }

    @PutMapping("/{id}")
    public SeatTypePrice update(@PathVariable Long id, @Valid @RequestBody SeatTypePriceRequest request) {
        return seatTypePriceService.update(id, request);
    }
}
