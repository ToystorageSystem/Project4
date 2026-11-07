
package com.toystorage.backend.services.auth;

import com.toystorage.backend.dto.response.auth.LoginResponse;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.repository.users.UserRepository;
import com.toystorage.backend.repository.users.UserTwoFactorRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoginCompletionService {

    private final PendingLoginService pendingLoginService;

    private final TwoFactorConfirmService twoFactorConfirmService;

    private final TwoFactorVerifyService twoFactorVerifyService;

    private final UserRepository userRepository;

    private final UserTwoFactorRepository userTwoFactorRepository;

    private final UserDetailsService userDetailsService;

    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    /*
     * Hoan tat dang nhap trong lan Setup 2FA dau tien.
     *
     * Chi tao SecurityContext sau khi OTP duoc
     * xac minh va transaction commit thanh cong.
     */
    public LoginResponse confirmSetupAndLogin(
            HttpServletRequest request,
            HttpServletResponse response,
            String otpCode
    ) {

        twoFactorConfirmService.confirm(
                request,
                otpCode
        );

        return completeLogin(
                request,
                response,
                PendingLoginService.Step.SETUP_2FA
        );
    }

    /*
     * Hoan tat dang nhap khi tai khoan da bat 2FA.
     */
    public LoginResponse verifyOtpAndLogin(
            HttpServletRequest request,
            HttpServletResponse response,
            String otpCode
    ) {

        twoFactorVerifyService.verify(
                request,
                otpCode
        );

        return completeLogin(
                request,
                response,
                PendingLoginService.Step.VERIFY_OTP
        );
    }

    /*
     * Tao phien dang nhap day du sau khi OTP hop le.
     *
     * Private method de khong cho phep
     * goi truc tiep bo qua xac minh OTP.
     */
    private LoginResponse completeLogin(
            HttpServletRequest request,
            HttpServletResponse response,
            PendingLoginService.Step expectedStep
    ) {

        // 1. Kiem tra phien dang nhap tam.
        PendingLoginService.PendingLogin pending =
                pendingLoginService.requireStep(
                        request,
                        expectedStep
                );

        Long userId = pending.userId();

        // 2. Lay thong tin tai khoan.
        Users user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Authentication session is invalid."
                        )
                );

        // 3. Kiem tra yeu cau doi mat khau.
        if (Boolean.TRUE.equals(
                user.getMustChangePassword()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Password change is required."
            );
        }

        // 4. Kiem tra tai khoan da bat 2FA.
        boolean enabledTwoFactor = userTwoFactorRepository
                .findById(userId)
                .map(twoFactor -> twoFactor.isEnabled())
                .orElse(false);

        if (!enabledTwoFactor) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Two-factor authentication is required."
            );
        }

        // 5. Tai lai UserDetails va quyen hien tai.
        UserDetails principal =
                userDetailsService.loadUserByUsername(
                        user.getEmail()
                );

        // 6. Kiem tra trang thai tai khoan.
        if (!principal.isEnabled()
                || !principal.isAccountNonLocked()
                || !principal.isAccountNonExpired()
                || !principal.isCredentialsNonExpired()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Account is not available for sign-in."
            );
        }

        // 7. Tao Authentication sau khi OTP thanh cong.
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        // 8. Kiem tra session tam con ton tai.
        HttpSession session = request.getSession(false);

        if (session == null) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication session expired. Sign in again."
            );
        }

        // 9. Doi session ID de chong session fixation.
        request.changeSessionId();

        // 10. Xoa thong tin dang nhap tam.
        pendingLoginService.clearPending(request);

        // 11. Thoi gian session dang nhap day du.
        session.setMaxInactiveInterval(15 * 60);

        // 12. Tao SecurityContext moi.
        SecurityContext securityContext =
                SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
                authentication
        );

        SecurityContextHolder.setContext(
                securityContext
        );

        // 13. Luu SecurityContext vao session.
        securityContextRepository.saveContext(
                securityContext,
                request,
                response
        );

        // 14. Lay danh sach quyen cua tai khoan.
        Set<String> authorities = principal
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // 15. Tra ve ket qua dang nhap.
        return LoginResponse.builder()
                .id(user.getId())
                .userCode(user.getUserCode())
                .name(user.getName())
                .email(user.getEmail())
                .authorities(authorities)
                .mustChangePassword(false)
                .loginStatus("AUTHENTICATED")
                .build();
    }
}
