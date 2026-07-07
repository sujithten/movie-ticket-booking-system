package com.example.movietickets.service;

import com.example.movietickets.dto.request.ScreenRequest;
import com.example.movietickets.entity.Screen;
import com.example.movietickets.exception.ResourceNotFoundException;
import com.example.movietickets.repository.ScreenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScreenService {

    private final ScreenRepository screenRepository;

    public List<Screen> getAll(Long theaterId) {
        return theaterId == null ? screenRepository.findAll() : screenRepository.findByTheaterId(theaterId);
    }

    public Screen create(ScreenRequest request) {
        return screenRepository.save(new Screen(null, request.theaterId(), request.name(), request.totalSeats()));
    }

    public Screen update(Long id, ScreenRequest request) {
        Screen screen = screenRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Screen " + id + " not found"));
        screen.setTheaterId(request.theaterId());
        screen.setName(request.name());
        screen.setTotalSeats(request.totalSeats());
        return screenRepository.save(screen);
    }

    public void delete(Long id) {
        if (!screenRepository.existsById(id)) {
            throw new ResourceNotFoundException("Screen " + id + " not found");
        }
        screenRepository.deleteById(id);
    }
}
