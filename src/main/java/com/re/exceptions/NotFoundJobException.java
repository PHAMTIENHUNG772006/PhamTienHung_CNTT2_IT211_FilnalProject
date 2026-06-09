package com.re.exceptions;

public class NotFoundJobException extends RuntimeException {
    public NotFoundJobException(String message) {
        super(message);
    }
}
