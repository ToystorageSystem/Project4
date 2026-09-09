package com.toystorage.backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.AccessDeniedException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class Global {


    // =========================================================
    // 400 - BAD REQUEST
    // =========================================================

    /*
     * Dùng cho exception tự tạo:
     *
     * throw new BadRequest("...");
     */
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


    // =========================================================
    // 400 - VALIDATION ERROR
    // =========================================================

    /*
     * Xử lý lỗi từ @Valid.
     *
     * Ví dụ:
     *
     * @NotBlank
     * @Email
     * @Size
     *
     * Nếu frontend gửi:
     *
     * email = "abc"
     *
     * thì trả 400 thay vì 500.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex
    ) {

        Map<String, String> errors =
                new LinkedHashMap<>();


        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->

                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );


        Map<String, Object> response =
                new LinkedHashMap<>();

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
                "Dữ liệu không hợp lệ"
        );

        response.put(
                "validationErrors",
                errors
        );


        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }


    // =========================================================
    // 400 - JSON BODY ERROR
    // =========================================================

    /*
     * Xử lý:
     *
     * - Không gửi request body
     * - JSON sai cú pháp
     * - JSON parse lỗi
     *
     * Trước đây trường hợp này đang bị trả 500.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(
            HttpMessageNotReadableException ex
    ) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                "Request body bị thiếu hoặc JSON không đúng định dạng"
        );
    }


    // =========================================================
    // 401 - UNAUTHORIZED
    // =========================================================

    /*
     * Dùng cho exception Unauthorized
     * của project.
     *
     * Ví dụ:
     *
     * - Sai email
     * - Sai password
     * - Account không hoạt động
     */
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


    // =========================================================
    // 403 - CUSTOM FORBIDDEN
    // =========================================================

    /*
     * Dùng khi code của mình chủ động:
     *
     * throw new Forbidden("...");
     */
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


    // =========================================================
    // 403 - SPRING SECURITY ACCESS DENIED
    // =========================================================

    /*
     * QUAN TRỌNG.
     *
     * Đây là lỗi mình vừa gặp.
     *
     * Ví dụ:
     *
     * BUSINESS_STAFF đã login
     *
     * nhưng gọi:
     *
     * /api/admin/users
     *
     * Controller có:
     *
     * @PreAuthorize("hasRole('ADMIN')")
     *
     * Spring sẽ throw AccessDeniedException.
     *
     * Nếu không có handler này:
     *
     * AccessDeniedException
     *      ↓
     * Exception.class
     *      ↓
     * 500
     *
     * Sau khi thêm:
     *
     * AccessDeniedException
     *      ↓
     * 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>>
    handleAccessDenied(
            AccessDeniedException ex
    ) {

        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                "Bạn không có quyền thực hiện chức năng này"
        );
    }


    // =========================================================
    // 404 - NOT FOUND
    // =========================================================

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


    // =========================================================
    // 500 - INTERNAL SERVER ERROR
    // =========================================================

    /*
     * Handler cuối cùng.
     *
     * Chỉ những lỗi thật sự chưa được xử lý
     * mới rơi xuống đây.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(
            Exception ex
    ) {

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                ex.getMessage()
        );
    }


    // =========================================================
    // BUILD RESPONSE
    // =========================================================

    /*
     * Hàm dùng chung để tránh lặp code.
     */
    private ResponseEntity<Map<String, Object>> buildResponse(
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


        return ResponseEntity
                .status(status)
                .body(response);
    }
}