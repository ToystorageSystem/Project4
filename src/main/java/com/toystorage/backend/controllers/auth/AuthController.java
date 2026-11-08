package com.toystorage.backend.controllers.auth;

import com.toystorage.backend.dto.request.auth.LoginRequest;
import com.toystorage.backend.dto.request.auth.MfaRequests;
import com.toystorage.backend.dto.response.auth.LoginResponse;
import com.toystorage.backend.services.auth.AuthService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService service;
    @GetMapping("/csrf")
    public Map<String,String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest http) {
        return service.login(body, http);
    }
    @GetMapping({"/status", "/me"})
    public LoginResponse status(HttpServletRequest http) { return service.status(http); }
    @PostMapping("/change-password")
    public LoginResponse changePassword(@Valid @RequestBody MfaRequests.ChangePassword body, HttpServletRequest http) {
        return service.changePassword(body, http);
    }

    @PostMapping("/2fa/recover")
    public LoginResponse recover(@Valid @RequestBody MfaRequests.Recovery body, HttpServletRequest http) {
        return service.recover(body.code(), http);
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest http, HttpServletResponse output) {
        if (http.getSession(false) != null) http.getSession(false).invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}
