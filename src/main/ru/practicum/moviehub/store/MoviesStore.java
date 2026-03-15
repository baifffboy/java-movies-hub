package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.LinkedHashMap;
import java.util.Map;

public class MoviesStore {
    private static LinkedHashMap<Integer, Movie> moviesList = new LinkedHashMap<>();

    public static Map<Integer, Movie> getMoviesMap() {
        return moviesList;
    }

    public static void addMovie(Movie movie) {
        int newId = moviesList.isEmpty() ? 1 : moviesList.lastEntry().getKey() + 1;
        moviesList.put(newId, movie);
    }
}