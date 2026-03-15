package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MoviesHandlerWithID extends BaseHttpHandler {
    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        setResponseHeaders(ex);

        if (ex.getRequestURI().getPath().split("/").length != 3
                || ex.getRequestURI().getPath().split("/")[2].isEmpty()) {
            responseErrorGeneration(
                    "400 Bad Request",
                    new ArrayList<>(List.of("Некорректный ID")),
                    400,
                    ex);
            return;
        }

        switch (method) {
            case "GET" -> {
                getHeadersAndBodyOfMovies(ex);
            }
            case "DELETE" -> {
                deleteMovie(ex);
            }
            default -> {
                responseErrorGeneration(
                        "405 Bad Method",
                        new ArrayList<>(List.of("Данный метод не обрабатывается сервером")),
                        405,
                        ex);
            }
        }
    }

    private void deleteMovie(HttpExchange ex) throws IOException {
        int id = parseId(ex);
        if (id == -1) return;
        Movie movie = MoviesStore.getMoviesMap().remove(id);
        if (movie == null) {
            responseErrorGeneration(
                    "404 Not Found",
                    new ArrayList<>(List.of("Фильм не найден")),
                    404,
                    ex);
            return;
        }
        ex.sendResponseHeaders(204, -1);
        ex.getResponseBody().close();
    }

    public void getHeadersAndBodyOfMovies(HttpExchange ex) throws IOException {
        int id = parseId(ex);
        if (id == -1) return;
        Movie movie = MoviesStore.getMoviesMap().get(id);
        if (movie == null) {
            responseErrorGeneration(
                    "404 Not Found",
                    new ArrayList<>(List.of("Фильм не найден")),
                    404,
                    ex);
            return;
        }
        String response = storeToJSONMovie(movie);
        sendHeadersAndResponse(response, ex, 200);
    }

    public String storeToJSONMovie(Movie movie) {
        return gson.toJson(movie);
    }

    public int parseId(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String idString = path.split("/")[2];
        try {
            return Integer.parseInt(idString);
        } catch (NumberFormatException e) {
            responseErrorGeneration(
                    "400 Bad Request",
                    new ArrayList<>(List.of("Некорректный ID")),
                    400,
                    ex);
            return -1;
        }
    }
}
