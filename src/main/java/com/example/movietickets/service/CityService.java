package com.example.movietickets.service;

import com.example.movietickets.dto.request.CityRequest;
import com.example.movietickets.entity.City;
import com.example.movietickets.exception.ResourceNotFoundException;
import com.example.movietickets.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;

    public List<City> getAll() {
        return cityRepository.findAll();
    }

    public City create(CityRequest request) {
        return cityRepository.save(new City(null, request.name()));
    }

    public City update(Long id, CityRequest request) {
        City city = cityRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("City " + id + " not found"));
        city.setName(request.name());
        return cityRepository.save(city);
    }

    public void delete(Long id) {
        if (!cityRepository.existsById(id)) {
            throw new ResourceNotFoundException("City " + id + " not found");
        }
        cityRepository.deleteById(id);
    }
}
