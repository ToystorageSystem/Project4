
package com.toystorage.backend.controllers.auth;

import com.toystorage.backend.dto.request.auth.ChangePasswordRequest;
import com.toystorage.backend.dto.response.auth.LoginResponse;
import com.toystorage.backend.dto.response.auth.TotpSetupResponse;

import com.toystorage.backend.services.auth.LoginCompletionService;
import com.toystorage.backend.services.auth.PendingPasswordChangeService;
import com.toystorage.backend.services.auth.TwoFactorSetupService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class TwoFactorController {

    private final PendingPasswordChangeService
            pendingPasswordChangeService;

    private final TwoFactorSetupService
            twoFactorSetupService;

    private final LoginCompletionService
            loginCompletionService;

    /*
     * Doi mat khau bat buoc trong phien tam.
     *
     * Chi hoat dong neu PendingLogin
     * dang o buoc CHANGE_PASSWORD.
     */
    @PatchMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest body,
            HttpServletRequest request
    ) {

        String nextStatus =
                pendingPasswordChangeService
                        .changeRequiredPassword(
                                request,
                                body
                        );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of(
                        "loginStatus",
                        nextStatus
                ));
    }

    /*
     * Bat dau thiet lap Google Authenticator.
     *
     * Chi cho phep khi PendingLogin
     * dang o buoc SETUP_2FA.
     *
     * Tra ve URI de tao QR va manualEntryKey.
     */
    @PostMapping("/2fa/setup")
    public ResponseEntity<TotpSetupResponse> setup(
            HttpServletRequest request
    ) {

        TotpSetupResponse result =
                twoFactorSetupService.setup(request);

        // Khong cache thong tin chua TOTP secret.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Pragma", "no-cache")
                .body(result);
    }

    /*
     * Xac nhan OTP trong lan setup dau tien.
     *
     * OTP dung:
     * - Kich hoat 2FA.
     * - Hoan tat dang nhap.
     * - Tra ve authorities.
     */
    @PostMapping("/2fa/confirm")
    public ResponseEntity<LoginResponse> confirm(
            @RequestBody Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        LoginResponse result =
                loginCompletionService.confirmSetupAndLogin(
                        request,
                        response,
                        body.get("code")
                );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(result);
    }

    /*
     * Xac minh OTP cua tai khoan da bat 2FA.
     *
     * OTP dung:
     * - Ghi nhan chu ky OTP.
     * - Hoan tat dang nhap.
     * - Tra ve authorities.
     */
    @PostMapping("/2fa/verify")
    public ResponseEntity<LoginResponse> verify(
            @RequestBody Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        LoginResponse result =
                loginCompletionService.verifyOtpAndLogin(
                        request,
                        response,
                        body.get("code")
                );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(result);
    }
}
