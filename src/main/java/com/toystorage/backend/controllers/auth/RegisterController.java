package com.toystorage.backend.controllers.auth;

import com.toystorage.backend.dto.request.auth.RegisterRequest;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.services.auth.RegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RegisterController {

    private final RegisterService registerService;

    @PostMapping("/register")
    public ResponseEntity<Users> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(
                registerService.register(request)
        );
    }
}