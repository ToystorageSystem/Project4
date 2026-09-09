package com.toystorage.backend.config.security;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.users.UserStatus;
import com.toystorage.backend.repository.users.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Component;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MustChangePasswordFilter
        extends OncePerRequestFilter {

    private final UserRepository userRepository;


    @Override
    protected void doFilterInternal(

            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain

    ) throws ServletException, IOException {

        String path =
                request.getRequestURI();


        // =====================================================
        // PUBLIC / ALLOWED PATH
        // =====================================================

        /*
         * Login phải được phép.
         */
        if (
                path.equals(
                        "/api/auth/login"
                )
        ) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }


        /*
         * User bắt buộc phải được phép gọi
         * Change Password.
         */
        if (
                path.equals(
                        "/api/auth/change-password"
                )
        ) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }


        /*
         * Nếu project có Logout thì cho phép
         * user logout dù đang bị bắt đổi password.
         */
        if (
                path.equals(
                        "/api/auth/logout"
                )
        ) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }


        /*
         * Swagger.
         */
        if (
                path.startsWith("/swagger")
                        || path.startsWith("/v3/api-docs")
        ) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }


        /*
         * OPTIONS request.
         */
        if (
                "OPTIONS".equalsIgnoreCase(
                        request.getMethod()
                )
        ) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }


        // =====================================================
        // AUTHENTICATION
        // =====================================================

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        if (
                authentication == null
                        || !authentication.isAuthenticated()
        ) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }


        Object principal =
                authentication.getPrincipal();


        if (
                !(principal instanceof CustomUserDetails userDetails)
        ) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }


        // =====================================================
        // LOAD USER MỚI NHẤT TỪ DB
        // =====================================================

        Optional<Users> optionalUser =
                userRepository.findById(
                        userDetails.getId()
                );


        if (optionalUser.isEmpty()) {

            writeUnauthorized(
                    response,
                    "Tài khoản không tồn tại"
            );

            return;
        }


        Users user =
                optionalUser.get();


        // =====================================================
        // ACCOUNT STATUS
        // =====================================================

        /*
         * Defense-in-depth:
         *
         * dù session vẫn còn vì lý do nào đó,
         * LOCKED/INACTIVE vẫn không được sử dụng API.
         */
        if (
                user.getStatus()
                        != UserStatus.ACTIVE
        ) {

            writeUnauthorized(
                    response,
                    "Tài khoản không còn hoạt động"
            );

            return;
        }


        // =====================================================
        // MUST CHANGE PASSWORD
        // =====================================================

        if (
                user.isMustChangePassword()
        ) {

            response.setStatus(
                    HttpServletResponse.SC_FORBIDDEN
            );

            response.setContentType(
                    "application/json"
            );

            response.setCharacterEncoding(
                    "UTF-8"
            );


            response.getWriter().write(
                    "{"
                            + "\"status\":403,"
                            + "\"error\":\"Password Change Required\","
                            + "\"code\":\"PASSWORD_CHANGE_REQUIRED\","
                            + "\"message\":\"Bạn phải đổi mật khẩu trước khi sử dụng hệ thống\""
                            + "}"
            );

            return;
        }


        filterChain.doFilter(
                request,
                response
        );
    }


    private void writeUnauthorized(
            HttpServletResponse response,
            String message
    ) throws IOException {

        response.setStatus(
                HttpServletResponse.SC_UNAUTHORIZED
        );

        response.setContentType(
                "application/json"
        );

        response.setCharacterEncoding(
                "UTF-8"
        );

        response.getWriter().write(
                "{"
                        + "\"status\":401,"
                        + "\"error\":\"Unauthorized\","
                        + "\"message\":\""
                        + message
                        + "\""
                        + "}"
        );
    }
}