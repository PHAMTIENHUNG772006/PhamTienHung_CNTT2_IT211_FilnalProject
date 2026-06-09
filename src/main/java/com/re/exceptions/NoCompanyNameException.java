package com.re.exceptions;

public class NoCompanyNameException extends RuntimeException {
    public NoCompanyNameException(String message) {
        super(message);
    }
}
