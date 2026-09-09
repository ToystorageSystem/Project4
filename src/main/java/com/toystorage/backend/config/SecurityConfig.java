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

    /*
     * Load user, role, permission khi login.
     */
    private final CustomUserDetailsService customUserDetailsService;


    /*
     * Filter kiểm tra:
     *
     * mustChangePassword = true
     *
     * thì user chỉ được:
     *
     * - login
     * - change-password
     * - logout
     *
     * và bị chặn những API khác.
     */
    private final MustChangePasswordFilter mustChangePasswordFilter;


    // =========================================================
    // PASSWORD ENCODER
    // =========================================================

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }


    // =========================================================
    // SESSION REGISTRY
    // =========================================================

    /*
     * Theo dõi những session đang hoạt động.
     *
     * Ví dụ:
     *
     * User 15
     *      ↓
     * JSESSIONID ABC123
     *
     * Khi Admin khóa User 15,
     * UserSessionService tìm được session này
     * và expire nó.
     */
    @Bean
    public SessionRegistry sessionRegistry() {

        return new SessionRegistryImpl();
    }


    // =========================================================
    // HTTP SESSION EVENT
    // =========================================================

    /*
     * Giúp SessionRegistry nhận biết session:
     *
     * - timeout
     * - invalidate
     * - destroy
     *
     * để loại session khỏi registry đúng cách.
     */
    @Bean
    public ServletListenerRegistrationBean<HttpSessionEventPublisher>
    httpSessionEventPublisher() {

        return new ServletListenerRegistrationBean<>(
                new HttpSessionEventPublisher()
        );
    }


    // =========================================================
    // AUTHENTICATION PROVIDER
    // =========================================================

    @Bean
    public AuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider();


        /*
         * Spring Security lấy user
         * từ CustomUserDetailsService.
         */
        provider.setUserDetailsService(
                customUserDetailsService
        );


        /*
         * Password trong DB sử dụng BCrypt.
         */
        provider.setPasswordEncoder(
                passwordEncoder()
        );


        return provider;
    }


    // =========================================================
    // AUTHENTICATION MANAGER
    // =========================================================

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration
                .getAuthenticationManager();
    }


    // =========================================================
    // SECURITY FILTER CHAIN
    // =========================================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                // =================================================
                // CSRF
                // =================================================

                /*
                 * Hiện project dùng REST API + session.
                 *
                 * Giai đoạn phát triển hiện tại đang disable CSRF.
                 */
                .csrf(csrf ->
                        csrf.disable()
                )


                // =================================================
                // AUTHENTICATION PROVIDER
                // =================================================

                .authenticationProvider(
                        authenticationProvider()
                )


                // =================================================
                // AUTHORIZATION
                // =================================================

                .authorizeHttpRequests(auth ->

                        auth

                                /*
                                 * Các endpoint public.
                                 */
                                .requestMatchers(

                                        "/api/auth/login",

                                        "/swagger",

                                        "/swagger/**",

                                        "/swagger-ui/**",

                                        "/v3/api-docs/**"
                                )

                                .permitAll()


                                /*
                                 * Mọi endpoint khác
                                 * phải đăng nhập.
                                 */
                                .anyRequest()
                                .authenticated()
                )


                // =================================================
                // SESSION MANAGEMENT
                // =================================================

                .sessionManagement(session ->

                        session

                                /*
                                 * Chỉ tạo HttpSession
                                 * khi thực sự cần.
                                 */
                                .sessionCreationPolicy(
                                        SessionCreationPolicy.IF_REQUIRED
                                )


                                /*
                                 * Một user chỉ có tối đa
                                 * một session hoạt động.
                                 */
                                .maximumSessions(1)


                                /*
                                 * Đăng ký session với
                                 * SessionRegistry.
                                 */
                                .sessionRegistry(
                                        sessionRegistry()
                                )


                                /*
                                 * Khi session đã bị Admin revoke
                                 * mà client vẫn dùng JSESSIONID cũ:
                                 *
                                 * trả 401.
                                 */
                                .expiredSessionStrategy(event -> {

                                    var response =
                                            event.getResponse();


                                    response.setStatus(
                                            401
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
                                                    + "\"message\":\"Phiên đăng nhập đã bị thu hồi\""
                                                    + "}"
                                    );
                                })

                )


                // =================================================
                // MUST CHANGE PASSWORD FILTER
                // =================================================

                /*
                 * SecurityContext phải được load trước.
                 *
                 * Sau đó filter của mình mới đọc được:
                 *
                 * SecurityContextHolder
                 *      ↓
                 * Authentication
                 *      ↓
                 * CustomUserDetails
                 *
                 * Nếu:
                 *
                 * mustChangePassword = true
                 *
                 * thì các API nghiệp vụ sẽ bị chặn.
                 */
                .addFilterAfter(
                        mustChangePasswordFilter,
                        SecurityContextHolderFilter.class
                )


                // =================================================
                // SECURITY EXCEPTION
                // =================================================

                .exceptionHandling(exception ->

                        exception

                                /*
                                 * Chưa login /
                                 * session không hợp lệ.
                                 */
                                .authenticationEntryPoint(
                                        (request, response, ex) -> {

                                            response.setStatus(
                                                    401
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
                                                            + "\"message\":\"Bạn chưa đăng nhập hoặc phiên đăng nhập không hợp lệ\""
                                                            + "}"
                                            );
                                        }
                                )


                                /*
                                 * Đã login nhưng không có quyền.
                                 *
                                 * Ví dụ:
                                 *
                                 * BUSINESS_STAFF
                                 * gọi /api/admin/users
                                 */
                                .accessDeniedHandler(
                                        (request, response, ex) -> {

                                            response.setStatus(
                                                    403
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
                                                            + "\"error\":\"Forbidden\","
                                                            + "\"message\":\"Bạn không có quyền thực hiện chức năng này\""
                                                            + "}"
                                            );
                                        }
                                )
                );


        return http.build();
    }
}