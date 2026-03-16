package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Collectors;

public class MoviesStore {
    private LinkedHashMap<Integer, Movie> moviesMap;

    public MoviesStore() {
        moviesMap = new LinkedHashMap<>();
    }

    public void addMovie(Movie movie) {
        int newId = moviesMap.isEmpty() ? 1 : moviesMap.lastEntry().getKey() + 1;
        moviesMap.put(newId, movie);
    }

    public Collection<Movie> getMovies() {
        return moviesMap.values();
    }

    public Movie getMovieById(int id) {
        return moviesMap.get(id);
    }

    public Movie deleteMovie(int id) {
        Movie mov = getMovieById(id);
        moviesMap.remove(id);
        return mov;
    }

    public void clear() {
        moviesMap.clear();
    }

    public Set<Movie> filterMoviesByYear(int year) {
        return moviesMap.values().stream()
                .filter(movie -> movie.year() == year)
                .collect(Collectors.toSet());
    }

    public LinkedHashMap<Integer, Movie> getMoviesMap() {
        return moviesMap;
    }
}