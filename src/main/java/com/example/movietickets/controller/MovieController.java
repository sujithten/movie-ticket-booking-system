package com.example.movietickets.controller;

import com.example.movietickets.dto.request.MovieRequest;
import com.example.movietickets.entity.Movie;
import com.example.movietickets.service.MovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @GetMapping
    public List<Movie> getAll(@RequestParam(required = false) Long cityId,
                               @RequestParam(required = false) Long theaterId) {
        return movieService.getAll(cityId, theaterId);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Movie create(@Valid @RequestBody MovieRequest request) {
        return movieService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Movie update(@PathVariable Long id, @Valid @RequestBody MovieRequest request) {
        return movieService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        movieService.delete(id);
    }
}
