package com.example.movietickets.controller;

import com.example.movietickets.dto.request.CityRequest;
import com.example.movietickets.entity.City;
import com.example.movietickets.service.CityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cities")
@RequiredArgsConstructor
public class CityController {

    private final CityService cityService;

    @GetMapping
    public List<City> getAll() {
        return cityService.getAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public City create(@Valid @RequestBody CityRequest request) {
        return cityService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public City update(@PathVariable Long id, @Valid @RequestBody CityRequest request) {
        return cityService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        cityService.delete(id);
    }
}
