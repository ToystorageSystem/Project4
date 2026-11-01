
package com.toystorage.backend.config;

import com.toystorage.backend.config.security.CustomUserDetailsService;
import com.toystorage.backend.config.security.MustChangePasswordFilter;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final MustChangePasswordFilter mustChangePasswordFilter;

    // =====================================================
    // PASSWORD ENCODER
    // =====================================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // =====================================================
    // SESSION REGISTRY
    // Theo doi cac phien dang nhap dang hoat dong.
    // =====================================================

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    // =====================================================
    // HTTP SESSION EVENT
    // Dong bo trang thai session voi SessionRegistry.
    // =====================================================

    @Bean
    public ServletListenerRegistrationBean<HttpSessionEventPublisher>
            httpSessionEventPublisher() {

        return new ServletListenerRegistrationBean<>(
                new HttpSessionEventPublisher()
        );
    }

    // =====================================================
    // AUTHENTICATION PROVIDER
    // Xac thuc tai khoan bang UserDetailsService va BCrypt.
    // =====================================================

    @Bean
    public AuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider();

        provider.setUserDetailsService(
                customUserDetailsService
        );

        provider.setPasswordEncoder(
                passwordEncoder()
        );

        return provider;
    }

    // =====================================================
    // AUTHENTICATION MANAGER
    // =====================================================

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration.getAuthenticationManager();
    }

    // =====================================================
    // SECURITY FILTER CHAIN
    // =====================================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                // Giu nguyen cau hinh CSRF cua project.
                .csrf(csrf -> csrf.disable())

                // Su dung AuthenticationProvider hien tai.
                .authenticationProvider(
                        authenticationProvider()
                )

                // =================================================
                // AUTHORIZATION
                // =================================================

                .authorizeHttpRequests(auth -> auth

                        // Cac endpoint khong can dang nhap.
                        .requestMatchers(
                                "/api/auth/login",
                                "/swagger",
                                "/swagger/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        )
                        .permitAll()

                        // Cac endpoint con lai phai dang nhap.
                        .anyRequest()
                        .authenticated()
                )

                // =================================================
                // SESSION MANAGEMENT
                // =================================================

                .sessionManagement(session -> session

                        .sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )

                        // Moi tai khoan toi da mot session.
                        .maximumSessions(1)

                        .sessionRegistry(
                                sessionRegistry()
                        )

                        // Tra HTTP 401 khi session bi thu hoi.
                        .expiredSessionStrategy(event -> {

                            var response = event.getResponse();

                            response.setStatus(401);
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
                                    + "\"message\":\"Your session has been revoked.\""
                                    + "}"
                            );
                        })
                )

                // =================================================
                // MUST CHANGE PASSWORD FILTER
                // Giu nguyen thu tu filter.
                // =================================================

                .addFilterAfter(
                        mustChangePasswordFilter,
                        SecurityContextHolderFilter.class
                )

                // =================================================
                // SECURITY EXCEPTION HANDLING
                // =================================================

                .exceptionHandling(exception -> exception

                        // -----------------------------------------
                        // 401 - UNAUTHORIZED
                        // Chua dang nhap hoac session khong hop le.
                        // -----------------------------------------

                        .authenticationEntryPoint(
                                (request, response, ex) -> {

                                    response.setStatus(401);
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
                                            + "\"message\":\"You are not authenticated or your session is invalid.\""
                                            + "}"
                                    );
                                }
                        )

                        // -----------------------------------------
                        // 403 - FORBIDDEN
                        // Da dang nhap nhung khong du quyen.
                        // -----------------------------------------

                        .accessDeniedHandler(
                                (request, response, ex) -> {

                                    response.setStatus(403);
                                    response.setContentType(
                                            "application/json"
                                    );
                                    response.setCharacterEncoding(
                                            "UTF-8"
                                    );

                                    response.getWriter().write(
                                            "{"
                                            + "\"status\":403,"
                                            + "\"error\":\"Forbidden\","
                                            + "\"message\":\"You do not have permission to perform this action.\""
                                            + "}"
                                    );
                                }
                        )
                );

        return http.build();
    }
}
