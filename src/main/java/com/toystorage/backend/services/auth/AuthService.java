
package com.toystorage.backend.services.auth;

import com.toystorage.backend.dto.request.auth.LoginRequest;
import com.toystorage.backend.dto.response.auth.LoginResponse;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.repository.users.UserRepository;
import com.toystorage.backend.repository.users.UserTwoFactorRepository;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;

    private final UserRepository userRepository;

    private final UserTwoFactorRepository userTwoFactorRepository;

    private final PendingLoginService pendingLoginService;

    // =====================================================
    // LOGIN - PASSWORD AUTHENTICATION
    // =====================================================

    @Transactional(readOnly = true)
    public LoginResponse login(
            LoginRequest request,
            HttpServletRequest httpRequest
    ) {

        // Buoc 1: Xac thuc email va mat khau.
        // Chua cap SecurityContext hoac quyen truy cap.

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Buoc 2: Lay thong tin tai khoan.

        Users user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(
                        () -> new NotFound("User not found.")
                );

        // Buoc 3: Kiem tra doi mat khau lan dau.

        boolean mustChangePassword =
                Boolean.TRUE.equals(
                        user.getMustChangePassword()
                );

        // Buoc 4: Kiem tra trang thai Google Authenticator.

        boolean enabledTwoFactor =
                userTwoFactorRepository
                        .findById(user.getId())
                        .map(twoFactor -> twoFactor.isEnabled())
                        .orElse(false);

        // Buoc 5: Xac dinh buoc xac thuc tiep theo.

        PendingLoginService.Step nextStep;

        String loginStatus;

        if (mustChangePassword) {

            nextStep =
                    PendingLoginService.Step.CHANGE_PASSWORD;

            loginStatus = "REQUIRE_CHANGE_PASSWORD";

        } else if (!enabledTwoFactor) {

            nextStep =
                    PendingLoginService.Step.SETUP_2FA;

            loginStatus = "REQUIRE_2FA_SETUP";

        } else {

            nextStep =
                    PendingLoginService.Step.VERIFY_OTP;

            loginStatus = "REQUIRE_2FA_VERIFICATION";
        }

        // Buoc 6: Tao phien cho xac thuc.
        // Khong luu password, OTP hoac quyen nghiep vu.

        pendingLoginService.begin(
                httpRequest,
                user.getId(),
                nextStep
        );

        // Buoc 7: Tra ve thong tin va trang thai Login.
        // Authorities rong cho den khi OTP thanh cong.

        return LoginResponse.builder()
                .id(user.getId())
                .userCode(user.getUserCode())
                .name(user.getName())
                .email(user.getEmail())
                .authorities(Set.of())
                .mustChangePassword(mustChangePassword)
                .loginStatus(loginStatus)
                .build();
    }
}
