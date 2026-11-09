package com.toystorage.backend.exceptions;

public class Conflict extends RuntimeException {

    public Conflict(String message) {
        super(message);
    }
}