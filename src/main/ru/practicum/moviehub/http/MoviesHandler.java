package ru.practicum.moviehub.http;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {
    private MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String queryString = ex.getRequestURI().getQuery();
        setResponseHeaders(ex);

        switch (method) {
            case "GET" -> {
                if (queryString == null) getHeadersAndBodyOfMovies(ex);
                else if (queryString.startsWith("year=")) {
                    String yearString = queryString.split("=")[1];
                    int yearOfMovie;
                    try {
                        yearOfMovie = Integer.parseInt(yearString);
                        getHeadersAndBodyOfMoviesWithYearFilter(ex, yearOfMovie);
                    } catch (NumberFormatException e) {
                        responseErrorGeneration(
                                "400 Bad Request",
                                new ArrayList<>(List.of("Некорректно указан год")),
                                400,
                                ex);
                    }
                }
            }
            case "POST" -> postHeadersAndBodyOfMovies(ex);
            default -> responseErrorGeneration(
                    "405 Bad Method",
                    new ArrayList<>(List.of("Данный метод не обрабатывается сервером")),
                    405,
                    ex);
        }
    }

    public void getHeadersAndBodyOfMovies(HttpExchange ex) throws IOException {
        String response = storeToJSONWithoutID();
        sendHeadersAndResponse(response, ex, 200);
    }

    public void getHeadersAndBodyOfMoviesWithYearFilter(HttpExchange ex, int year) throws IOException {
        String response = storeToJSONWithoutIDWithYearFilter(year);
        sendHeadersAndResponse(response, ex, 200);
    }

    public String storeToJSONWithoutID() {
        return gson.toJson(store.getMovies());
    }

    public String storeToJSONWithoutIDWithYearFilter(int year) {
        return gson.toJson(store.filterMoviesByYear(year));
    }

    public String storeToJSONWithID() {
        int counter = store.getMovies().size();
        return gson.toJson(store.getMovieById(counter));
    }

    public void postHeadersAndBodyOfMovies(HttpExchange ex) throws IOException {
        String contentTypeList = ex.getRequestHeaders().getFirst("Content-Type");
        if (contentTypeList == null || !contentTypeList.equals("application/json; charset=UTF-8")) {
            responseErrorGeneration(
                    "415 Unsupported Media Type",
                    new ArrayList<>(List.of("Заголовок Content-Type должен содержать тип JSON")),
                    415,
                    ex);
            return;
        }

        String contentLength = ex.getRequestHeaders().getFirst("Content-Length");
        if (Integer.parseInt(contentLength) == 0) {
            responseErrorGeneration(
                    "422 Unprocessable Entity",
                    new ArrayList<>(List.of("Пустое тело запроса")),
                    422,
                    ex);
            return;
        }

        Movie movieThatNeedPublic;
        try (InputStreamReader isr = new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8)) {
            movieThatNeedPublic = storeFromJSON(isr);
        } catch (JsonSyntaxException | IOException e) {
            responseErrorGeneration(
                    "422 Unprocessable Entity",
                    new ArrayList<>(List.of("Некорректно переданный JSON файл")),
                    422,
                    ex);
            return;
        }

        if (movieThatNeedPublic.year() < 1888 || movieThatNeedPublic.year() >= Year.now().getValue() + 1) {
            responseErrorGeneration(
                    "422 Unprocessable Entity",
                    new ArrayList<>(List.of("Несуществующий год существования фильма")),
                    422,
                    ex);
            return;
        } else if (movieThatNeedPublic.title().length() > 100 || movieThatNeedPublic.title().isBlank()) {
            responseErrorGeneration(
                    "422 Unprocessable Entity",
                    new ArrayList<>(List.of("Название не должно быть пустым", "Название не должно быть более 100 символов")),
                    422,
                    ex);
            return;
        }

        addMoviesInStore(movieThatNeedPublic);
        String response = storeToJSONWithID();
        sendHeadersAndResponse(response, ex, 201);
    }

    public Movie storeFromJSON(InputStreamReader isr) {
        return gson.fromJson(isr, Movie.class);
    }

    public void addMoviesInStore(Movie movie) {
        store.addMovie(movie);
    }
}
