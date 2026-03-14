package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class MoviesHandlerWithID extends BaseHttpHandler{
    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        setResponseHeaders(ex);

        if (ex.getRequestURI().getPath().split("/").length != 3) {
            String response = gson.toJson(
                    new ErrorResponse("400 Bad Request",
                            new ArrayList<>(Arrays.asList("ID не указан"))));
            sendHeadersAndResponse(response, ex, 400);
            return;
        }

        switch(method){
            case "GET" -> {
                getHeadersAndBodyOfMovies(ex);
            }
            case "DELETE" -> {
                deleteMovie(ex);
            }
            default -> {
                String response = gson.toJson(
                    new ErrorResponse("405 Bad Method",
                            (ArrayList<String>) Arrays.asList("Данный метод не обрабатывается сервером")));
                sendHeadersAndResponse(response, ex, 405);
            }
        }
    }

    private void deleteMovie(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String id_string = path.split("/")[2];
        int id;
        try {
            id = Integer.parseInt(id_string);
        } catch (NumberFormatException e) {
            String response = gson.toJson(
                    new ErrorResponse("400 Bad Request",
                            (ArrayList<String>) Arrays.asList("Некорректный ID")));
            sendHeadersAndResponse(response, ex, 400);
            return;
        }
        Movie movie = MoviesStore.getMoviesMap().remove(id);
        if (movie == null) {
            String response = gson.toJson(
                    new ErrorResponse("404 Not Found",
                            (ArrayList<String>) Arrays.asList("Фильм не найден")));
            sendHeadersAndResponse(response, ex, 404);
            return;
        }
        ex.sendResponseHeaders(204, -1);
        ex.getResponseBody().close();
    }

    public void getHeadersAndBodyOfMovies(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String id_string = path.split("/")[2];
        int id;
        try {
            id = Integer.parseInt(id_string);
        } catch (NumberFormatException e) {
            String response = gson.toJson(
                    new ErrorResponse("400 Bad Request",
                            (ArrayList<String>) Arrays.asList("Некорректный ID")));
            sendHeadersAndResponse(response, ex, 400);
            return;
        }
        Movie movie = MoviesStore.getMoviesMap().get(id);
        if (movie == null) {
            String response = gson.toJson(
                    new ErrorResponse("404 Not Found",
                            (ArrayList<String>) Arrays.asList("Фильм не найден")));
            sendHeadersAndResponse(response, ex, 404);
            return;
        }
        String response = storeToJSONMovie(movie);
        sendHeadersAndResponse(response, ex, 200);
    }

    public String storeToJSONMovie(Movie movie) {
        return gson.toJson(movie);
    }

}
