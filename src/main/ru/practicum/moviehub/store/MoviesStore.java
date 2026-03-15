package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MoviesStore {
    private static LinkedHashMap<Integer, Movie> moviesMap = new LinkedHashMap<>();

    public static Map<Integer, Movie> getMoviesMap() {
        return moviesMap;
    }

    public static void addMovie(Movie movie) {
        int newId = moviesMap.isEmpty() ? 1 : moviesMap.lastEntry().getKey() + 1;
        moviesMap.put(newId, movie);
    }

    public static Set<Movie> filterMoviesByYear(int year) {
        return moviesMap.values().stream()
                .filter(movie -> movie.getYear() == year)
                .collect(Collectors.toSet());
    }
}