package com.example.movietickets.controller;

import com.example.movietickets.dto.request.TheaterRequest;
import com.example.movietickets.entity.Theater;
import com.example.movietickets.service.TheaterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/theaters")
@RequiredArgsConstructor
public class TheaterController {

    private final TheaterService theaterService;

    @GetMapping
    public List<Theater> getAll(@RequestParam(required = false) Long cityId) {
        return theaterService.getAll(cityId);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Theater create(@Valid @RequestBody TheaterRequest request) {
        return theaterService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Theater update(@PathVariable Long id, @Valid @RequestBody TheaterRequest request) {
        return theaterService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        theaterService.delete(id);
    }
}
