package com.example.movietickets.service;

import com.example.movietickets.dto.request.TheaterRequest;
import com.example.movietickets.entity.Theater;
import com.example.movietickets.exception.ResourceNotFoundException;
import com.example.movietickets.repository.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TheaterService {

    private final TheaterRepository theaterRepository;

    public List<Theater> getAll(Long cityId) {
        return cityId == null ? theaterRepository.findAll() : theaterRepository.findByCityId(cityId);
    }

    public Theater create(TheaterRequest request) {
        return theaterRepository.save(new Theater(null, request.name(), request.cityId(), request.address()));
    }

    public Theater update(Long id, TheaterRequest request) {
        Theater theater = theaterRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Theater " + id + " not found"));
        theater.setName(request.name());
        theater.setCityId(request.cityId());
        theater.setAddress(request.address());
        return theaterRepository.save(theater);
    }

    public void delete(Long id) {
        if (!theaterRepository.existsById(id)) {
            throw new ResourceNotFoundException("Theater " + id + " not found");
        }
        theaterRepository.deleteById(id);
    }
}
