package com.toystorage.backend.controllers.auth;

import com.toystorage.backend.dto.request.auth.MfaRequests;
import com.toystorage.backend.dto.response.auth.LoginResponse;
import com.toystorage.backend.services.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthProtectionController {

    private final AuthService service;

    @GetMapping("/2fa/setup")
    public AuthService.SetupResponse setup(HttpServletRequest request) {
        return service.setup(request);
    }

    @PostMapping({"/2fa/confirm", "/2fa/setup/confirm"})
    public LoginResponse confirm(
            @Valid @RequestBody MfaRequests.Otp body,
            HttpServletRequest request,
            HttpServletResponse response) {

        return service.confirm(body.code(), request, response);
    }

    @PostMapping("/2fa/verify")
    public LoginResponse verify(
            @Valid @RequestBody MfaRequests.Otp body,
            HttpServletRequest request,
            HttpServletResponse response) {

        return service.verify(body.code(), request, response);
    }
}