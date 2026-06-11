package com.re.exceptions;

public class NotFoundApplicationException extends RuntimeException {
    public NotFoundApplicationException(String message) {
        super(message);
    }
}
