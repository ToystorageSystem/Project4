package com.toystorage.backend.controllers.auth;
import com.toystorage.backend.dto.request.auth.PasswordResetRequests;
import com.toystorage.backend.dto.response.auth.LoginResponse;
import com.toystorage.backend.services.auth.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {
    private final PasswordResetService service;
    @PostMapping("/forgot-password")
    public Map<String,String> forgot(@Valid @RequestBody PasswordResetRequests.Forgot body) {
        service.forgot(body.email());
        return Map.of("message","Neu email da duoc dang ky, huong dan dat lai mat khau se duoc gui.");
    }
    @PostMapping("/reset-password")
    public LoginResponse reset(@Valid @RequestBody PasswordResetRequests.Reset body,HttpServletRequest http) {
        return service.reset(body,http);
    }
}
