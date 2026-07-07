package com.example.movietickets.service;

import com.example.movietickets.dto.request.MovieRequest;
import com.example.movietickets.entity.Movie;
import com.example.movietickets.entity.Screen;
import com.example.movietickets.entity.Theater;
import com.example.movietickets.exception.ResourceNotFoundException;
import com.example.movietickets.repository.MovieRepository;
import com.example.movietickets.repository.ScreenRepository;
import com.example.movietickets.repository.ShowRepository;
import com.example.movietickets.repository.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;
    private final ScreenRepository screenRepository;
    private final TheaterRepository theaterRepository;

    /** Browse movies currently showing, optionally scoped to a city/theater. */
    public List<Movie> getAll(Long cityId, Long theaterId) {
        if (cityId == null && theaterId == null) {
            return movieRepository.findAll();
        }

        List<Long> theaterIds = theaterId != null
            ? List.of(theaterId)
            : theaterRepository.findByCityId(cityId).stream().map(Theater::getId).toList();

        List<Long> screenIds = theaterIds.stream()
            .flatMap(tid -> screenRepository.findByTheaterId(tid).stream())
            .map(Screen::getId)
            .toList();

        if (screenIds.isEmpty()) {
            return List.of();
        }

        List<Long> movieIds = showRepository.findDistinctMovieIdsByScreenIdIn(screenIds);
        return movieRepository.findAllById(movieIds);
    }

    public Movie create(MovieRequest request) {
        return movieRepository.save(new Movie(null, request.title(), request.durationMinutes(),
            request.language(), request.genre()));
    }

    public Movie update(Long id, MovieRequest request) {
        Movie movie = movieRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Movie " + id + " not found"));
        movie.setTitle(request.title());
        movie.setDurationMinutes(request.durationMinutes());
        movie.setLanguage(request.language());
        movie.setGenre(request.genre());
        return movieRepository.save(movie);
    }

    public void delete(Long id) {
        if (!movieRepository.existsById(id)) {
            throw new ResourceNotFoundException("Movie " + id + " not found");
        }
        movieRepository.deleteById(id);
    }
}
