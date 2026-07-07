package com.example.movietickets.service;

import com.example.movietickets.dto.request.SeatTypePriceRequest;
import com.example.movietickets.entity.SeatTypePrice;
import com.example.movietickets.exception.ResourceNotFoundException;
import com.example.movietickets.repository.SeatTypePriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatTypePriceService {

    private final SeatTypePriceRepository seatTypePriceRepository;

    public List<SeatTypePrice> getAll() {
        return seatTypePriceRepository.findAll();
    }

    public SeatTypePrice create(SeatTypePriceRequest request) {
        return seatTypePriceRepository.save(new SeatTypePrice(null, request.seatType(), request.basePrice()));
    }

    public SeatTypePrice update(Long id, SeatTypePriceRequest request) {
        SeatTypePrice price = seatTypePriceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("SeatTypePrice " + id + " not found"));
        price.setSeatType(request.seatType());
        price.setBasePrice(request.basePrice());
        return seatTypePriceRepository.save(price);
    }
}
