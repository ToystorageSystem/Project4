package com.toystorage.backend.config;

import com.toystorage.backend.config.security.*;
import com.toystorage.backend.repository.users.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.session.*;
import org.springframework.security.web.csrf.*;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.*;
import java.util.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean public AuthenticationProvider authenticationProvider(CustomUserDetailsService details, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(details);
        provider.setPasswordEncoder(encoder);
        return provider;
    }
    @Bean public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
    @Bean public SessionRegistry sessionRegistry() { return new SessionRegistryImpl(); }
    @Bean public HttpSessionEventPublisher httpSessionEventPublisher() { return new HttpSessionEventPublisher(); }
    @Bean public HttpSessionCsrfTokenRepository csrfTokenRepository() { return new HttpSessionCsrfTokenRepository(); }
    @Bean public SessionAuthenticationStrategy loginSessionStrategy(SessionRegistry registry, HttpSessionCsrfTokenRepository csrf) {
        var concurrency = new ConcurrentSessionControlAuthenticationStrategy(registry);
        concurrency.setMaximumSessions(1);
        return new CompositeSessionAuthenticationStrategy(List.of(concurrency,
                new ChangeSessionIdAuthenticationStrategy(), new RegisterSessionAuthenticationStrategy(registry),
                new CsrfAuthenticationStrategy(csrf)));
    }
    @Bean public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.security.allowed-origins:http://localhost:5173}") String origins) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN", "Accept"));
        cors.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
    @Bean public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationProvider provider,
            SessionRegistry registry, HttpSessionCsrfTokenRepository csrf, UserRepository users) throws Exception {
        http.cors(cors -> {})
            .csrf(config -> config.csrfTokenRepository(csrf)
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .authenticationProvider(provider)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/forgot-password", "/api/auth/reset-password",
                        "/api/auth/csrf", "/api/auth/login", "/api/auth/logout", "/api/auth/status",
                        "/api/auth/change-password", "/api/auth/2fa/setup", "/api/auth/2fa/confirm",
                        "/api/auth/2fa/verify", "/api/auth/2fa/recover", "/api/auth/2fa/setup/confirm", "/api/auth/me",
                        "/swagger", "/swagger/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .maximumSessions(1).sessionRegistry(registry)
                    .expiredSessionStrategy(event -> event.getResponse().setStatus(401)))
            .exceptionHandling(errors -> errors.authenticationEntryPoint((req, res, ex) -> res.setStatus(401))
                    .accessDeniedHandler((req, res, ex) -> res.setStatus(403)))
            .addFilterBefore(new MfaSessionGuard(users), AuthorizationFilter.class);
        return http.build();
    }
}
