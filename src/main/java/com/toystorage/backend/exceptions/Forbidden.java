package com.toystorage.backend.exceptions;

public class Forbidden extends RuntimeException {
    public Forbidden(String message) {
        super(message);
    }
}
