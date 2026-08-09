package com.toystorage.backend.services.auth;

import com.toystorage.backend.dto.request.auth.LoginRequest;
import com.toystorage.backend.dto.response.auth.LoginResponse;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.repository.users.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public LoginResponse login(
            LoginRequest request,
            HttpServletRequest httpRequest
    ) {

        /*
         * Spring Security kiểm tra:
         *
         * email
         * password
         * account status
         */

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getEmail(),
                                request.getPassword()
                        )
                );

        /*
         * Tạo SecurityContext
         */

        SecurityContext securityContext =
                SecurityContextHolder
                        .createEmptyContext();

        securityContext.setAuthentication(
                authentication
        );

        SecurityContextHolder.setContext(
                securityContext
        );

        /*
         * Tạo HTTP Session.
         *
         * Session này sẽ timeout sau 15 phút
         * không có request.
         */

        HttpSession session =
                httpRequest.getSession(true);

        session.setAttribute(
                HttpSessionSecurityContextRepository
                        .SPRING_SECURITY_CONTEXT_KEY,
                securityContext
        );

        /*
         * Không bắt buộc nếu đã cấu hình properties,
         * nhưng set trực tiếp giúp rõ ràng.
         */

        session.setMaxInactiveInterval(
                15 * 60
        );

        Users user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new NotFound(
                                "User not found"
                        )
                );

        Set<String> authorities =
                authentication
                        .getAuthorities()
                        .stream()
                        .map(authority ->
                                authority.getAuthority()
                        )
                        .collect(Collectors.toSet());

        return LoginResponse.builder()
                .id(user.getId())
                .userCode(user.getUserCode())
                .name(user.getName())
                .email(user.getEmail())
                .authorities(authorities)
                .build();
    }
}