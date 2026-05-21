package ru.practicum.moviehub.api;

import java.util.ArrayList;

public record ErrorResponse(String error, ArrayList<String> details) {
}