package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.store.MoviesStore;

public abstract class BaseHttpHandler implements HttpHandler {
    protected final Gson gson = new Gson();
}