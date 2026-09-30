package com.toystorage.backend.services.auth;

import com.toystorage.backend.config.security.CustomUserDetails;

import com.toystorage.backend.dto.request.auth.ChangePasswordRequest;
import com.toystorage.backend.dto.request.auth.LoginRequest;

import com.toystorage.backend.dto.response.auth.LoginResponse;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.users.ActivityAction;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;

import com.toystorage.backend.repository.users.UserRepository;

import com.toystorage.backend.services.users.ActivityLogService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.core.session.SessionRegistry;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;

    private final UserRepository userRepository;

    private final SessionRegistry sessionRegistry;

    private final PasswordEncoder passwordEncoder;

    private final ActivityLogService activityLogService;


    // =========================================================
    // LOGIN
    // =========================================================

    @Transactional(readOnly = true)
    public LoginResponse login(

            LoginRequest request,

            HttpServletRequest httpRequest
    ) {

        Authentication authentication;


        // =====================================================
        // 1. AUTHENTICATE
        // =====================================================

        try {

            authentication =
                    authenticationManager.authenticate(

                            new UsernamePasswordAuthenticationToken(
                                    request.getEmail(),
                                    request.getPassword()
                            )
                    );

        } catch (AuthenticationException ex) {


            // =================================================
            // LOGIN FAILED
            //
            // Không ghi password.
            //
            // logAuthenticationAttempt sử dụng REQUIRES_NEW
            // nên log vẫn được commit trước khi throw Unauthorized.
            // =================================================

            activityLogService
                    .logAuthenticationAttempt(

                            request.getEmail(),

                            httpRequest,

                            ActivityAction.LOGIN_FAILED
                    );


            throw new Unauthorized(
                    "Email hoặc mật khẩu không đúng, hoặc tài khoản không hoạt động"
            );
        }


        // =====================================================
        // 2. SECURITY CONTEXT
        // =====================================================

        SecurityContext securityContext =
                SecurityContextHolder
                        .createEmptyContext();


        securityContext.setAuthentication(
                authentication
        );


        SecurityContextHolder.setContext(
                securityContext
        );


        // =====================================================
        // 3. HTTP SESSION
        // =====================================================

        HttpSession session =
                httpRequest.getSession(
                        true
                );


        session.setAttribute(

                HttpSessionSecurityContextRepository
                        .SPRING_SECURITY_CONTEXT_KEY,

                securityContext
        );


        // 15 phút không hoạt động
        session.setMaxInactiveInterval(
                15 * 60
        );


        // =====================================================
        // 4. REGISTER SESSION
        // =====================================================

        sessionRegistry
                .registerNewSession(

                        session.getId(),

                        authentication.getPrincipal()
                );


        // =====================================================
        // 5. USER
        // =====================================================

        Users user =
                userRepository
                        .findByEmail(
                                request.getEmail()
                        )
                        .orElseThrow(
                                () ->
                                        new NotFound(
                                                "Không tìm thấy tài khoản"
                                        )
                        );


        // =====================================================
        // 6. LOGIN SUCCESS ACTIVITY LOG
        //
        // Chỉ ghi:
        //
        // attemptedEmail
        // result = SUCCESS
        //
        // Không ghi password.
        // =====================================================

        activityLogService
                .logAuthenticationAttempt(

                        request.getEmail(),

                        httpRequest,

                        ActivityAction.LOGIN
                );


        // =====================================================
        // 7. AUTHORITIES
        // =====================================================

        Set<String> authorities =
                authentication
                        .getAuthorities()
                        .stream()

                        .map(
                                authority ->
                                        authority.getAuthority()
                        )

                        .collect(
                                Collectors.toSet()
                        );


        // =====================================================
        // 8. RESPONSE
        // =====================================================

        return LoginResponse
                .builder()

                .id(
                        user.getId()
                )

                .userCode(
                        user.getUserCode()
                )

                .name(
                        user.getName()
                )

                .email(
                        user.getEmail()
                )

                .authorities(
                        authorities
                )

                .mustChangePassword(
                        user.isMustChangePassword()
                )

                .build();
    }


    // =========================================================
    // CHANGE PASSWORD
    // =========================================================

    @Transactional
    public void changePassword(

            ChangePasswordRequest request,

            Authentication authentication
    ) {

        // =====================================================
        // 1. CHECK AUTHENTICATION
        // =====================================================

        if (
                authentication == null
                        || !authentication.isAuthenticated()
        ) {

            throw new Unauthorized(
                    "Bạn chưa đăng nhập"
            );
        }


        Object principal =
                authentication.getPrincipal();


        if (
                !(principal instanceof CustomUserDetails userDetails)
        ) {

            throw new Unauthorized(
                    "Phiên đăng nhập không hợp lệ"
            );
        }


        Long userId =
                userDetails.getId();


        Users user =
                userRepository
                        .findById(
                                userId
                        )
                        .orElseThrow(
                                () ->
                                        new NotFound(
                                                "Không tìm thấy tài khoản"
                                        )
                        );


        // =====================================================
        // 2. CHECK OLD PASSWORD
        // =====================================================

        boolean oldPasswordCorrect =
                passwordEncoder.matches(

                        request.getOldPassword(),

                        user.getPassword()
                );


        if (!oldPasswordCorrect) {

            throw new BadRequest(
                    "Mật khẩu hiện tại không đúng"
            );
        }


        // =====================================================
        // 3. NEW PASSWORD MUST DIFFER
        // =====================================================

        boolean samePassword =
                passwordEncoder.matches(

                        request.getNewPassword(),

                        user.getPassword()
                );


        if (samePassword) {

            throw new BadRequest(
                    "Mật khẩu mới phải khác mật khẩu hiện tại"
            );
        }


        // =====================================================
        // 4. OLD VALUE
        //
        // TUYỆT ĐỐI KHÔNG ghi:
        //
        // - oldPassword
        // - newPassword
        // - password hash
        // =====================================================

        Map<String, Object> oldValue =
                new LinkedHashMap<>();


        oldValue.put(
                "mustChangePassword",
                user.isMustChangePassword()
        );


        // =====================================================
        // 5. SAVE NEW PASSWORD
        // =====================================================

        user.setPassword(

                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );


        user.setMustChangePassword(
                false
        );


        Users savedUser =
                userRepository.save(
                        user
                );


        // =====================================================
        // 6. NEW VALUE
        // =====================================================

        Map<String, Object> newValue =
                new LinkedHashMap<>();


        newValue.put(
                "passwordChanged",
                true
        );


        newValue.put(
                "mustChangePassword",
                savedUser.isMustChangePassword()
        );


        // =====================================================
        // 7. ACTIVITY LOG
        // =====================================================

        activityLogService.log(

                authentication,

                getCurrentHttpRequest(),

                ActivityAction.CHANGE_PASSWORD,

                com.toystorage.backend.enums.users.ActivityEntityType.USER,

                savedUser.getId(),

                oldValue,

                newValue
        );
    }


    // =========================================================
    // GET CURRENT HTTP REQUEST
    // =========================================================

    private HttpServletRequest getCurrentHttpRequest() {

        if (
                RequestContextHolder
                        .getRequestAttributes()
                        instanceof ServletRequestAttributes attributes
        ) {

            return attributes.getRequest();
        }


        return null;
    }
}