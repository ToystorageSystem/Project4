
package com.toystorage.backend.services.auth;

import com.toystorage.backend.config.security.CustomUserDetails;

import com.toystorage.backend.dto.request.auth.ChangePasswordRequest;
import com.toystorage.backend.dto.request.auth.LoginRequest;
import com.toystorage.backend.dto.response.auth.LoginResponse;

import com.toystorage.backend.entity.users.ActivityLogs;
import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;

import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;

import com.toystorage.backend.repository.users.UserRepository;

import com.toystorage.backend.services.users.ActivityLogService;
import com.toystorage.backend.services.users.SuspiciousLoginDetectionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final SessionRegistry sessionRegistry;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;
    private final SuspiciousLoginDetectionService suspiciousLoginDetectionService;

    // =========================================================
    // LOGIN
    // =========================================================

    @Transactional(readOnly = true)
    public LoginResponse login(
            LoginRequest request,
            HttpServletRequest httpRequest
    ) {

        Authentication authentication;

        // Bước 1: Xác thực email và mật khẩu.
        try {

            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

        } catch (AuthenticationException ex) {

            // Bước 2: Ghi lại lần đăng nhập thất bại.
            // ActivityLogService dùng REQUIRES_NEW để lưu log
            // ngay cả khi đăng nhập bị từ chối.

            ActivityLogs failedLog =
                    activityLogService.logAuthenticationAttempt(
                            request.getEmail(),
                            httpRequest,
                            ActivityAction.LOGIN_FAILED
                    );

            // Bước 3: Phát hiện đăng nhập bất thường cho Task #27.
            // Lỗi phát hiện cảnh báo không làm thay đổi kết quả Login.

            try {

                suspiciousLoginDetectionService
                        .detectAfterFailedLogin(failedLog);

            } catch (RuntimeException detectionEx) {

                log.error(
                        "Failed to evaluate suspicious login for activity log ID: {}",
                        failedLog.getId(),
                        detectionEx
                );
            }

            throw new Unauthorized(
                    "Invalid email or password, or the account is inactive."
            );
        }

        // =====================================================
        // LOGIN SUCCESS
        // =====================================================

        // Bước 4: Tạo SecurityContext.

        SecurityContext securityContext =
                SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);

        SecurityContextHolder.setContext(securityContext);

        // Bước 5: Tạo và cấu hình Session.

        HttpSession session = httpRequest.getSession(true);

        session.setAttribute(
                HttpSessionSecurityContextRepository
                        .SPRING_SECURITY_CONTEXT_KEY,
                securityContext
        );

        // Session hết hạn sau 15 phút không hoạt động.
        session.setMaxInactiveInterval(15 * 60);

        // Bước 6: Đăng ký Session.

        sessionRegistry.registerNewSession(
                session.getId(),
                authentication.getPrincipal()
        );

        // Bước 7: Lấy thông tin người dùng.

        Users user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(
                        () -> new NotFound(
                                "User account not found."
                        )
                );

        // Bước 8: Ghi Activity Log đăng nhập thành công.

        activityLogService.logAuthenticationAttempt(
                request.getEmail(),
                httpRequest,
                ActivityAction.LOGIN
        );

        // Bước 9: Lấy danh sách quyền của người dùng.

        Set<String> authorities = authentication
                .getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toSet());

        // Bước 10: Trả về dữ liệu đăng nhập.

        return LoginResponse.builder()
                .id(user.getId())
                .userCode(user.getUserCode())
                .name(user.getName())
                .email(user.getEmail())
                .authorities(authorities)
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

        // Bước 1: Kiểm tra người dùng đã đăng nhập.

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new Unauthorized(
                    "You are not logged in."
            );
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CustomUserDetails userDetails)) {

            throw new Unauthorized(
                    "Invalid login session."
            );
        }

        Long userId = userDetails.getId();

        Users user = userRepository
                .findById(userId)
                .orElseThrow(
                        () -> new NotFound(
                                "User account not found."
                        )
                );

        // Bước 2: Kiểm tra mật khẩu hiện tại.

        boolean oldPasswordCorrect = passwordEncoder.matches(
                request.getOldPassword(),
                user.getPassword()
        );

        if (!oldPasswordCorrect) {

            throw new BadRequest(
                    "Current password is incorrect."
            );
        }

        // Bước 3: Mật khẩu mới phải khác mật khẩu cũ.

        boolean samePassword = passwordEncoder.matches(
                request.getNewPassword(),
                user.getPassword()
        );

        if (samePassword) {

            throw new BadRequest(
                    "New password must be different from the current password."
            );
        }

        // Bước 4: Lưu trạng thái trước khi thay đổi.
        // Tuyệt đối không lưu mật khẩu hoặc password hash.

        Map<String, Object> oldValue = new LinkedHashMap<>();

        oldValue.put(
                "mustChangePassword",
                user.isMustChangePassword()
        );

        // Bước 5: Mã hóa và lưu mật khẩu mới.

        user.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        user.setMustChangePassword(false);

        Users savedUser = userRepository.save(user);

        // Bước 6: Lưu trạng thái sau khi thay đổi.

        Map<String, Object> newValue = new LinkedHashMap<>();

        newValue.put("passwordChanged", true);

        newValue.put(
                "mustChangePassword",
                savedUser.isMustChangePassword()
        );

        // Bước 7: Ghi Activity Log.

        activityLogService.log(
                authentication,
                getCurrentHttpRequest(),
                ActivityAction.CHANGE_PASSWORD,
                ActivityEntityType.USER,
                savedUser.getId(),
                oldValue,
                newValue
        );
    }

    // =========================================================
    // GET CURRENT HTTP REQUEST
    // =========================================================

    private HttpServletRequest getCurrentHttpRequest() {

        if (RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes) {

            return attributes.getRequest();
        }

        return null;
    }
}
