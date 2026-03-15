package ru.practicum.moviehub.api;

import java.util.ArrayList;

public class ErrorResponse {
    private final String error;
    private final ArrayList<String> details;

    public ErrorResponse(String error, ArrayList<String> details) {
        this.error = error;
        this.details = details;
    }

    public String getError() {
        return error;
    }

    public ArrayList<String> getDetails() {
        return details;
    }
}