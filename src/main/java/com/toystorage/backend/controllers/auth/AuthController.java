package com.toystorage.backend.controllers.auth;

import com.toystorage.backend.dto.request.auth.ChangePasswordRequest;
import com.toystorage.backend.dto.request.auth.LoginRequest;
import com.toystorage.backend.dto.response.auth.LoginResponse;
import com.toystorage.backend.services.auth.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    // =========================================================
    // LOGIN
    // =========================================================

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(

            @Valid
            @RequestBody
            LoginRequest request,

            HttpServletRequest httpRequest
    ) {

        LoginResponse response =
                authService.login(
                        request,
                        httpRequest
                );


        return ResponseEntity.ok(
                response
        );
    }


    // =========================================================
    // CHANGE PASSWORD
    // =========================================================

    @PatchMapping("/change-password")
    public ResponseEntity<Void> changePassword(

            @Valid
            @RequestBody
            ChangePasswordRequest request,

            Authentication authentication
    ) {

        authService.changePassword(
                request,
                authentication
        );


        return ResponseEntity
                .noContent()
                .build();
    }
}