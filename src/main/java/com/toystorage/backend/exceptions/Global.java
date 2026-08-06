package com.toystorage.backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class Global {
    /*
    404 Not Found
     */
    @ExceptionHandler(NotFound.class)
    public ResponseEntity<Map<String, Object>>
    handleNotFound(NotFound ex){
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.NOT_FOUND);
        response.put("error", "Not Found");
        response.put("message", ex.getMessage());
        return new ResponseEntity<>(
                response,
                HttpStatus.NOT_FOUND
        );
    }
    /*
     * 400 BAD REQUEST
     */
    @ExceptionHandler(BadRequest.class)
    public ResponseEntity<Map<String, Object>>
    handleBadRequest(BadRequest ex){
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST);
        response.put("error", "Bad Request");
        response.put("message", ex.getMessage());

        return new ResponseEntity<>(
                response,
                HttpStatus.BAD_REQUEST
        );
    }
    /*
     * 401 UNAUTHORIZED
     */
    @ExceptionHandler(Unauthorized.class)

    public ResponseEntity<Map<String, Object>>
    handleUnauthorized(Unauthorized ex) {

        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", LocalDateTime.now());
        response.put("status", 401);
        response.put("error", "Unauthorized");
        response.put("message", ex.getMessage());

        return new ResponseEntity<>(
                response,
                HttpStatus.UNAUTHORIZED
        );
    }

    /*
     * 403 FORBIDDEN
     */
    @ExceptionHandler(Forbidden.class)

    public ResponseEntity<Map<String, Object>>
    handleForbidden(Forbidden ex) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("timestamp", LocalDateTime.now());
        response.put("status", 403);
        response.put("error", "Forbidden");
        response.put("message", ex.getMessage());

        return new ResponseEntity<>(
                response,
                HttpStatus.FORBIDDEN
        );
    }

    /*
     * 500 INTERNAL SERVER ERROR
     */
    @ExceptionHandler(Exception.class)

    public ResponseEntity<Map<String, Object>>
    handleException(Exception ex) {

        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", LocalDateTime.now());
        response.put("status", 500);
        response.put("error", "Internal Server Error");
        response.put("message", ex.getMessage());

        return new ResponseEntity<>(
                response,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

}