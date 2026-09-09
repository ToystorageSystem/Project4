package com.toystorage.backend.services.auth;

import com.toystorage.backend.config.security.CustomUserDetails;

import com.toystorage.backend.dto.request.auth.ChangePasswordRequest;
import com.toystorage.backend.dto.request.auth.LoginRequest;

import com.toystorage.backend.dto.response.auth.LoginResponse;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;

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

    /*
     * Ghi Activity Log.
     */
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

        try {

            authentication =
                    authenticationManager.authenticate(

                            new UsernamePasswordAuthenticationToken(
                                    request.getEmail(),
                                    request.getPassword()
                            )
                    );

        } catch (AuthenticationException ex) {

            throw new Unauthorized(
                    "Email hoặc mật khẩu không đúng, hoặc tài khoản không hoạt động"
            );
        }


        // =====================================================
        // SECURITY CONTEXT
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
        // HTTP SESSION
        // =====================================================

        HttpSession session =
                httpRequest.getSession(true);


        session.setAttribute(

                HttpSessionSecurityContextRepository
                        .SPRING_SECURITY_CONTEXT_KEY,

                securityContext
        );


        /*
         * Timeout 15 phút không hoạt động.
         */
        session.setMaxInactiveInterval(
                15 * 60
        );


        /*
         * Đăng ký session để Admin có thể
         * revoke session khi LOCK/INACTIVE.
         */
        sessionRegistry.registerNewSession(
                session.getId(),
                authentication.getPrincipal()
        );


        // =====================================================
        // USER
        // =====================================================

        Users user =
                userRepository
                        .findByEmail(
                                request.getEmail()
                        )
                        .orElseThrow(() ->
                                new NotFound(
                                        "Không tìm thấy tài khoản"
                                )
                        );


        // =====================================================
        // AUTHORITIES
        // =====================================================

        Set<String> authorities =
                authentication
                        .getAuthorities()
                        .stream()

                        .map(authority ->
                                authority.getAuthority()
                        )

                        .collect(
                                Collectors.toSet()
                        );


        // =====================================================
        // RESPONSE
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

                /*
                 * Frontend dùng field này để biết
                 * user có bị bắt đổi mật khẩu hay không.
                 */
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
        // 1. KIỂM TRA ĐĂNG NHẬP
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
                        .orElseThrow(() ->
                                new NotFound(
                                        "Không tìm thấy tài khoản"
                                )
                        );


        // =====================================================
        // 2. KIỂM TRA OLD PASSWORD
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
        // 3. KHÔNG CHO PASSWORD MỚI TRÙNG PASSWORD CŨ
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
        // 4. CHỤP TRẠNG THÁI TRƯỚC KHI ĐỔI
        // =====================================================

        /*
         * TUYỆT ĐỐI KHÔNG ghi:
         *
         * oldPassword
         * newPassword
         * password hash
         */

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


        /*
         * Đã đổi mật khẩu thành công
         * nên bỏ yêu cầu bắt buộc đổi password.
         */
        user.setMustChangePassword(
                false
        );


        Users savedUser =
                userRepository.save(
                        user
                );


        // =====================================================
        // 6. TẠO NEW VALUE CHO ACTIVITY LOG
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
        // 7. GHI ACTIVITY LOG
        // =====================================================

        activityLogService.log(

                /*
                 * Chính user đang đăng nhập
                 * là actor.
                 */
                authentication,

                /*
                 * Lấy request hiện tại để ghi
                 * IP + User-Agent.
                 */
                getCurrentHttpRequest(),

                ActivityAction.CHANGE_PASSWORD,

                ActivityEntityType.USER,

                /*
                 * User tự đổi password cho chính mình.
                 */
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