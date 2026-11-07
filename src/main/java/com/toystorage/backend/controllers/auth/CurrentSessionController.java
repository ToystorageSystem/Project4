
package com.toystorage.backend.controllers.auth;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class CurrentSessionController {

    // Kiem tra phien dang nhap hien tai.
    // Chi tra ve thong tin khi da xac thuc day du.

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentSession(
            Authentication authentication
    ) {

        // Chua hoan tat xac thuc thi khong cap quyen.
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required."
            );
        }

        // Lay quyen thuc te tu Spring Security.
        List<String> authorities =
                authentication.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of(
                        "username", authentication.getName(),
                        "authorities", authorities
                ));
    }
}
