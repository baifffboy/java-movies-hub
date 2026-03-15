package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public abstract class BaseHttpHandler implements HttpHandler {
    protected final Gson gson = new Gson();

    protected void setResponseHeaders(HttpExchange ex) {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
    }

    protected void sendHeadersAndResponse(String response, HttpExchange ex, int returnCode) throws IOException {
        setResponseHeaders(ex);
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(returnCode, responseBytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(responseBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void responseErrorGeneration(String error, ArrayList<String> details, int returnCode, HttpExchange ex) throws IOException {
        String response = gson.toJson(new ErrorResponse(error, details));
        sendHeadersAndResponse(response, ex, returnCode);
    }
}