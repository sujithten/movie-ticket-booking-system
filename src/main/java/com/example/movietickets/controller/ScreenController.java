package com.example.movietickets.controller;

import com.example.movietickets.dto.request.ScreenRequest;
import com.example.movietickets.entity.Screen;
import com.example.movietickets.service.ScreenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/screens")
@RequiredArgsConstructor
public class ScreenController {

    private final ScreenService screenService;

    @GetMapping
    public List<Screen> getAll(@RequestParam(required = false) Long theaterId) {
        return screenService.getAll(theaterId);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Screen create(@Valid @RequestBody ScreenRequest request) {
        return screenService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Screen update(@PathVariable Long id, @Valid @RequestBody ScreenRequest request) {
        return screenService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        screenService.delete(id);
    }
}
