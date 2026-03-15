package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.api.ErrorResponse;
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
import java.time.Year;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

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
        MoviesStore.getMoviesMap().clear();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    private void assertContentType(HttpResponse<String> resp) {
        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
    }

    private Collection<Movie> parseMovies(String json) {
        try (InputStreamReader isr = new InputStreamReader(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8)) {
            return gson.fromJson(isr, new MoviesTypeTokenCollection());
        } catch (Exception e) {
            fail("Не удалось распарсить JSON: " + e.getMessage());
            return null;
        }
    }

    private ErrorResponse parseError(String json) {
        return gson.fromJson(json, ErrorResponse.class);
    }

    //GET /movies

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        assertContentType(resp);

        Collection<Movie> movies = parseMovies(resp.body());
        assertTrue(movies.isEmpty(), "Должен вернуться пустой массив");
    }

    @Test
    void getMovies_whenNotEmpty_returnsNotEmptyArray() throws Exception {
        // Добавляем фильмы
        MoviesStore.addMovie(new Movie("Матрица", 1999));
        MoviesStore.addMovie(new Movie("Терминатор", 1984));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertContentType(resp);

        Collection<Movie> movies = parseMovies(resp.body());
        assertEquals(2, movies.size(), "Должно вернуться 2 фильма");

        assertTrue(movies.stream().anyMatch(m -> m.getTitle().equals("Матрица") && m.getYear() == 1999));
        assertTrue(movies.stream().anyMatch(m -> m.getTitle().equals("Терминатор") && m.getYear() == 1984));
    }

    //POST /movies

    @Test
    void postMovies_whenValidData_addsMovie() throws Exception {
        Movie newMovie = new Movie("Новый фильм", 2023);
        String requestBody = gson.toJson(newMovie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode(), "При создании должен вернуться 201");
        assertContentType(resp);

        // Проверяем, что фильм действительно добавился
        assertEquals(1, MoviesStore.getMoviesMap().size());

        // Проверяем, что ID присвоился
        Movie addedMovie = MoviesStore.getMoviesMap().values().iterator().next();
        assertNotNull(addedMovie);
        assertEquals("Новый фильм", addedMovie.getTitle());
        assertEquals(2023, addedMovie.getYear());
    }

    @Test
    void postMovies_whenTitleEmpty_returnsError() throws Exception {
        Movie invalidMovie = new Movie("", 2023);
        String requestBody = gson.toJson(invalidMovie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "Должен вернуться 422 Unprocessable Entity");
        assertContentType(resp);

        ErrorResponse error = parseError(resp.body());
        assertEquals("422 Unprocessable Entity", error.error());
        assertTrue(error.details().contains("Название не должно быть пустым"));

        // Проверяем, что фильм не добавился
        assertEquals(0, MoviesStore.getMoviesMap().size());
    }

    @Test
    void postMovies_whenTitleTooLong_returnsError() throws Exception {
        String longTitle = "a".repeat(101);
        Movie invalidMovie = new Movie(longTitle, 2023);
        String requestBody = gson.toJson(invalidMovie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        assertContentType(resp);

        ErrorResponse error = parseError(resp.body());
        assertEquals("422 Unprocessable Entity", error.error());
        assertTrue(error.details().contains("Название не должно быть более 100 символов"));

        assertEquals(0, MoviesStore.getMoviesMap().size());
    }

    @Test
    void postMovies_whenYearTooEarly_returnsError() throws Exception {
        Movie invalidMovie = new Movie("Старый фильм", 1800);
        String requestBody = gson.toJson(invalidMovie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        assertContentType(resp);

        ErrorResponse error = parseError(resp.body());
        assertEquals("422 Unprocessable Entity", error.error());
        assertTrue(error.details().contains("Несуществующий год существования фильма"));

        assertEquals(0, MoviesStore.getMoviesMap().size());
    }

    @Test
    void postMovies_whenYearTooLate_returnsError() throws Exception {
        int futureYear = Year.now().getValue() + 2;
        Movie invalidMovie = new Movie("Фильм из будущего", futureYear);
        String requestBody = gson.toJson(invalidMovie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        assertContentType(resp);

        ErrorResponse error = parseError(resp.body());
        assertEquals("422 Unprocessable Entity", error.error());
        assertTrue(error.details().contains("Несуществующий год существования фильма"));

        assertEquals(0, MoviesStore.getMoviesMap().size());
    }

    @Test
    void postMovies_whenWrongContentType_returnsError() throws Exception {
        Movie movie = new Movie("Матрица", 1999);
        String requestBody = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, resp.statusCode(), "Должен вернуться 415 Unsupported Media Type");
        assertContentType(resp);

        ErrorResponse error = parseError(resp.body());
        assertEquals("415 Unsupported Media Type", error.error());

        assertEquals(0, MoviesStore.getMoviesMap().size());
    }

    @Test
    void postMovies_whenInvalidJson_returnsError() throws Exception {
        String invalidJson = "{title: \"Матрица\", year: 1999,}"; // Невалидный JSON

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "Должен вернуться 422 Ошибка валидации файла");
        assertContentType(resp);

        assertEquals(0, MoviesStore.getMoviesMap().size());
    }

    //GET /movies/{id}

    @Test
    void getMovieById_whenExists_returnsMovie() throws Exception {
        // Добавляем фильм
        MoviesStore.addMovie(new Movie("Матрица", 1999));
        int movieId = MoviesStore.getMoviesMap().keySet().iterator().next();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/" + movieId))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertContentType(resp);

        Movie movie = gson.fromJson(resp.body(), Movie.class);
        assertEquals("Матрица", movie.getTitle());
        assertEquals(1999, movie.getYear());
    }

    @Test
    void getMovieById_whenNotExists_returnsError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/999"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());
        assertContentType(resp);

        ErrorResponse error = parseError(resp.body());
        assertEquals("404 Not Found", error.error());
        assertTrue(error.details().contains("Фильм не найден"));
    }

    @Test
    void getMovieById_whenIdNotNumber_returnsError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/abc"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        assertContentType(resp);

        ErrorResponse error = parseError(resp.body());
        assertEquals("400 Bad Request", error.error());
        assertTrue(error.details().contains("Некорректный ID"));
    }

    //DELETE /movies/{id}

    @Test
    void deleteMovieById_whenExists_deletesMovie() throws Exception {
        // Добавляем фильм
        MoviesStore.addMovie(new Movie("Матрица", 1999));
        int movieId = MoviesStore.getMoviesMap().keySet().iterator().next();

        assertEquals(1, MoviesStore.getMoviesMap().size());

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/" + movieId))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, resp.statusCode(), "При удалении должен вернуться 204 No Content");

        // Проверяем, что фильм удалился
        assertEquals(0, MoviesStore.getMoviesMap().size());
    }

    @Test
    void deleteMovieById_whenNotExists_returnsError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/999"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());
        assertContentType(resp);

        ErrorResponse error = parseError(resp.body());
        assertEquals("404 Not Found", error.error());
        assertTrue(error.details().contains("Фильм не найден"));
    }

    @Test
    void deleteMovieById_whenIdNotNumber_returnsError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/abc"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        assertContentType(resp);

        ErrorResponse error = parseError(resp.body());
        assertEquals("400 Bad Request", error.error());
        assertTrue(error.details().contains("Некорректный ID"));
    }

    //GET /movies?year=YYYY

    @Test
    void getMoviesByYear_whenExists_returnsMovies() throws Exception {
        // Добавляем фильмы разных лет
        MoviesStore.addMovie(new Movie("Матрица", 1999));
        MoviesStore.addMovie(new Movie("Терминатор", 1984));
        MoviesStore.addMovie(new Movie("Матрица 2", 1999));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies?year=1999"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertContentType(resp);

        Collection<Movie> movies = parseMovies(resp.body());
        assertEquals(2, movies.size(), "Должно быть 2 фильма 1999 года");

        assertTrue(movies.stream().allMatch(m -> m.getYear() == 1999));
    }

    @Test
    void getMoviesByYear_whenNoMovies_returnsEmptyArray() throws Exception {
        // Добавляем фильмы других лет
        MoviesStore.addMovie(new Movie("Матрица", 1999));
        MoviesStore.addMovie(new Movie("Терминатор", 1984));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies?year=2000"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertContentType(resp);

        Collection<Movie> movies = parseMovies(resp.body());
        assertTrue(movies.isEmpty(), "Должен вернуться пустой массив");
    }

    @Test
    void getMoviesByYear_whenYearNotNumber_returnsError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies?year=abc"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        assertContentType(resp);

        ErrorResponse error = parseError(resp.body());
        assertEquals("400 Bad Request", error.error());
    }

    //Общие тесты

    @Test
    void unsupportedMethod_returns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .HEAD()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(405, resp.statusCode(), "Неподдерживаемый метод должен вернуть 405");
    }
}