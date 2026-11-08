package com.toystorage.backend.controllers.auth;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {AuthController.class, PasswordResetController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthErrorAdvice {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> status(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("status", ex.getStatusCode().value(),
                "message", ex.getReason() == null ? "AUTH_ERROR" : ex.getReason()));
    }
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> authentication(AuthenticationException ex) {
        return ResponseEntity.status(401).body(Map.of("status", 401, "message", "INVALID_CREDENTIALS"));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> invalid(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(Map.of("status", 400, "message", "INVALID_INPUT"));
    }
}
