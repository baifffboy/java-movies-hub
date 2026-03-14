package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {

    private static MoviesServer server;
    private HttpClient client;
    private final Gson gson = new Gson();

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer(8080);
        server.start();
    }

    @BeforeEach
    void beforeEach() {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        // создаем запрос(request)
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .GET()
                .build();
        // отсылаем запрос(request)
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        Collection<Movie> movies;
        try(InputStreamReader isr = new InputStreamReader(
                new ByteArrayInputStream(resp.body().getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)){
            movies = gson.fromJson(isr, new MoviesTypeTokenCollection());
        }
        assertTrue(movies.isEmpty(),
                "Ожидается JSON-массив");
    }


}