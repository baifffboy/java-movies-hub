package ru.practicum.moviehub.api;

import java.util.ArrayList;

public class ErrorResponse{
    private final String error;
    private final ArrayList<String> details;

    public ErrorResponse(String shortMessage, ArrayList<String> log) {
        this.error = shortMessage;
        this.details = log;
    }

    public String getError() {
        return error;
    }

    public ArrayList<String> getDetails() {
        return details;
    }
}