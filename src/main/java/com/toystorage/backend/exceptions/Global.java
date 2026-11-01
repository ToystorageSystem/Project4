
package com.toystorage.backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.security.access.AccessDeniedException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class Global {

    // =====================================================
    // 400 - BAD REQUEST
    // Xu ly exception BadRequest do project tu tao.
    // =====================================================

    @ExceptionHandler(BadRequest.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(
            BadRequest ex
    ) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                ex.getMessage()
        );
    }

    // =====================================================
    // 400 - VALIDATION ERROR
    // Xu ly cac loi @Valid cua request.
    // =====================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex
    ) {

        // Luu loi chi tiet theo ten truong.
        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        Map<String, Object> response = new LinkedHashMap<>();

        response.put(
                "timestamp",
                LocalDateTime.now()
        );

        response.put(
                "status",
                HttpStatus.BAD_REQUEST.value()
        );

        response.put(
                "error",
                "Bad Request"
        );

        response.put(
                "message",
                "The request data is invalid."
        );

        response.put(
                "validationErrors",
                errors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    // =====================================================
    // 400 - INVALID JSON BODY
    // Xu ly JSON sai cu phap hoac body bi thieu.
    // =====================================================

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(
            HttpMessageNotReadableException ex
    ) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                "Request body is missing or contains invalid JSON."
        );
    }

    // =====================================================
    // 400 - INVALID URL PARAMETER
    // Xu ly tham so URL khong dung kieu du lieu.
    // =====================================================

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex
    ) {

        // Xu ly rieng conversationId cua AI Chat.
        if ("conversationId".equals(ex.getName())) {

            return buildResponse(
                    HttpStatus.BAD_REQUEST,
                    "Bad Request",
                    "Conversation ID must be a valid number."
            );
        }

        // Cac tham so khac khong dung kieu du lieu.
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                "Invalid value for parameter: " + ex.getName()
        );
    }

    // =====================================================
    // 401 - UNAUTHORIZED
    // Xu ly exception Unauthorized cua project.
    // =====================================================

    @ExceptionHandler(Unauthorized.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(
            Unauthorized ex
    ) {

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                ex.getMessage()
        );
    }

    // =====================================================
    // 403 - CUSTOM FORBIDDEN
    // Xu ly exception Forbidden cua project.
    // =====================================================

    @ExceptionHandler(Forbidden.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(
            Forbidden ex
    ) {

        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                ex.getMessage()
        );
    }

    // =====================================================
    // 403 - SPRING SECURITY ACCESS DENIED
    // Xu ly loi phan quyen tu @PreAuthorize.
    // =====================================================

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex
    ) {

        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                "You do not have permission to perform this action."
        );
    }

    // =====================================================
    // 404 - NOT FOUND
    // Xu ly tai nguyen khong ton tai hoac khong thuoc user.
    // =====================================================

    @ExceptionHandler(NotFound.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            NotFound ex
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Not Found",
                ex.getMessage()
        );
    }

    // =====================================================
    // 500 - INTERNAL SERVER ERROR
    // Khong lam lo chi tiet loi noi bo ra Frontend.
    // =====================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(
            Exception ex
    ) {

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected server error occurred."
        );
    }

    // =====================================================
    // BUILD ERROR RESPONSE
    // Tao cau truc JSON response thong nhat.
    // =====================================================

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String error,
            String message
    ) {

        Map<String, Object> response = new LinkedHashMap<>();

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

        return ResponseEntity
                .status(status)
                .body(response);
    }
}
