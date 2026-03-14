package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.*;

public class MoviesHandler extends BaseHttpHandler{
    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        switch(method){
            case "GET" -> {
                setResponseHeaders(ex);
                getHeadersAndBodyOfMovies(ex);
            }
            case "POST" -> {
                setResponseHeaders(ex);
                postHeadersAndBodyOfMovies(ex);
            }
        }
    }

    public void setResponseHeaders(HttpExchange ex) {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
    }

    public void getHeadersAndBodyOfMovies(HttpExchange ex) throws IOException {
        String response = storeToJSONWithoutID();
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(200, responseBytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    public String storeToJSONWithoutID() {
        return gson.toJson(MoviesStore.getMoviesMap().values());
    }

    public String storeToJSONWithID() {
        return gson.toJson(MoviesStore.getMoviesMap());
    }

    public void postHeadersAndBodyOfMovies(HttpExchange ex) throws IOException {
        Movie movieThatNeedPublic = storeFromJSON(ex);
        if (movieThatNeedPublic.getYear() < 1888 || movieThatNeedPublic.getYear() >= Year.now().getValue() + 1) {
            String response = gson.toJson(
                    new ErrorResponse("Ошибка валидации",
                            (ArrayList<String>) Arrays.asList("несуществующий год существования фильма")));
            sendHeadersAndResponse(response, ex, 422);
        } else if (movieThatNeedPublic.getTitle().length() > 100 || movieThatNeedPublic.getTitle().isBlank()) {
            String response = gson.toJson(
                    new ErrorResponse("Ошибка валидации",
                            (ArrayList<String>) Arrays.asList("название не должно быть пустым", "название не должно быть более 100 символов")));
            sendHeadersAndResponse(response, ex, 422);
        } else if (!ex.getRequestHeaders().get("Content-Type").equals("application/json; charset=UTF-8")) {
            String response = gson.toJson(
                    new ErrorResponse("Неподдерживаемый тип носителя",
                            (ArrayList<String>) Arrays.asList("заголовок Content-Type должен содержать тип JSON")));
            sendHeadersAndResponse(response, ex, 415);
        } else {
            addMoviesInStore(movieThatNeedPublic);
            String response = storeToJSONWithID();
            sendHeadersAndResponse(response, ex, 201);
        }
    }

    public void sendHeadersAndResponse(String response, HttpExchange ex, int returnCode) throws IOException {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(returnCode, responseBytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(responseBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Movie storeFromJSON(HttpExchange ex) {
        InputStreamReader isr = new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8);
        return gson.fromJson(isr, new MoviesTypeToken());
    }

    public void addMoviesInStore(Movie movie){
        MoviesStore.addMovie(movie);
    }
}
