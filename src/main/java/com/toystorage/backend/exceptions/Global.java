package com.toystorage.backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class Global {

    /*
     * 404 NOT FOUND
     */
    @ExceptionHandler(NotFound.class)
    public ResponseEntity<Map<String, Object>>
    handleNotFound(NotFound ex) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Not Found",
                ex.getMessage()
        );
    }

    /*
     * 400 BAD REQUEST
     */
    @ExceptionHandler(BadRequest.class)
    public ResponseEntity<Map<String, Object>>
    handleBadRequest(BadRequest ex) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                ex.getMessage()
        );
    }

    /*
     * 400 VALIDATION ERROR
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>>
    handleValidation(
            MethodArgumentNotValidException ex
    ) {
        String message =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .findFirst()
                        .map(error ->
                                error.getField()
                                        + ": "
                                        + error.getDefaultMessage()
                        )
                        .orElse(
                                "Dữ liệu gửi lên không hợp lệ"
                        );

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                message
        );
    }

    /*
     * 401 UNAUTHORIZED
     */
    @ExceptionHandler(Unauthorized.class)
    public ResponseEntity<Map<String, Object>>
    handleUnauthorized(Unauthorized ex) {

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                ex.getMessage()
        );
    }

    /*
     * 403 FORBIDDEN DO NGHIỆP VỤ
     */
    @ExceptionHandler(Forbidden.class)
    public ResponseEntity<Map<String, Object>>
    handleForbidden(Forbidden ex) {

        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                ex.getMessage()
        );
    }

    /*
     * 403 FORBIDDEN DO SPRING SECURITY
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>>
    handleAccessDenied(AccessDeniedException ex) {

        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                "Bạn không có quyền thực hiện chức năng này"
        );
    }

    /*
     * 500 INTERNAL SERVER ERROR
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>>
    handleException(Exception ex) {

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                ex.getMessage()
        );
    }

    private ResponseEntity<Map<String, Object>>
    buildResponse(
            HttpStatus status,
            String error,
            String message
    ) {
        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "timestamp",
                LocalDateTime.now()
        );
        response.put(
                "status",
                status.value()
        );
        response.put(
                "error",
                error
        );
        response.put(
                "message",
                message
        );

        return new ResponseEntity<>(
                response,
                status
        );
    }
}