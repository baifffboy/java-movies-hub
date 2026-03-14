package ru.practicum.moviehub.api;

import java.util.ArrayList;

public class ErrorResponse{
    private final String shortMessage;
    private final ArrayList<String> log;

    public ErrorResponse(String shortMessage, ArrayList<String> log) {
        this.shortMessage = shortMessage;
        this.log = log;
    }
}