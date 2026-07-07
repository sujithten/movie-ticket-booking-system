package com.example.movietickets.service;

import com.example.movietickets.dto.request.SeatRequest;
import com.example.movietickets.entity.Seat;
import com.example.movietickets.exception.ResourceNotFoundException;
import com.example.movietickets.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;

    public List<Seat> getAll(Long screenId) {
        return screenId == null ? seatRepository.findAll() : seatRepository.findByScreenId(screenId);
    }

    public Seat create(SeatRequest request) {
        return seatRepository.save(new Seat(null, request.screenId(), request.seatNumber(), request.seatType()));
    }

    public Seat update(Long id, SeatRequest request) {
        Seat seat = seatRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Seat " + id + " not found"));
        seat.setScreenId(request.screenId());
        seat.setSeatNumber(request.seatNumber());
        seat.setSeatType(request.seatType());
        return seatRepository.save(seat);
    }

    public void delete(Long id) {
        if (!seatRepository.existsById(id)) {
            throw new ResourceNotFoundException("Seat " + id + " not found");
        }
        seatRepository.deleteById(id);
    }
}
