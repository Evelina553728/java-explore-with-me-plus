package ru.practicum.main.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class LocationNotExistException extends RuntimeException {

    public LocationNotExistException(String message) {
        super(message);
    }
}