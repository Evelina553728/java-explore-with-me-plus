package ru.practicum.main.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AlreadyPublishedException extends RuntimeException {

    public AlreadyPublishedException(String message) {
        super(message);
    }
}