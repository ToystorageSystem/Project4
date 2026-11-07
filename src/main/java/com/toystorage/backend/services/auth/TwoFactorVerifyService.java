
package com.toystorage.backend.services.auth;

import com.toystorage.backend.entity.users.UserTwoFactor;
import com.toystorage.backend.repository.users.UserTwoFactorRepository;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.OptionalLong;

@Service
@RequiredArgsConstructor
public class TwoFactorVerifyService {

    private final PendingLoginService pendingLoginService;

    private final UserTwoFactorRepository userTwoFactorRepository;

    private final TotpSecretEncryptionService encryptionService;

    private final TotpService totpService;

    /*
     * Xác minh OTP khi tài khoản đã bật 2FA.
     *
     * Chỉ hoạt động khi người dùng:
     * - Đã xác thực email và mật khẩu.
     * - Đang có PendingLogin ở bước VERIFY_OTP.
     *
     * Service này chưa cấp SecurityContext.
     */
    @Transactional
    public void verify(
            HttpServletRequest request,
            String otpCode
    ) {

        // 1. Kiểm tra phiên đăng nhập tạm.
        PendingLoginService.PendingLogin pending =
                pendingLoginService.requireStep(
                        request,
                        PendingLoginService.Step.VERIFY_OTP
                );

        Long userId = pending.userId();

        // 2. Khóa cấu hình 2FA để tránh xác minh đồng thời.
        UserTwoFactor twoFactor = userTwoFactorRepository
                .findByUserIdForUpdate(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Two-factor authentication is not configured."
                        )
                );

        if (!twoFactor.isEnabled()
                || twoFactor.getEncryptedSecret() == null
                || twoFactor.getEncryptedSecret().length == 0) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Two-factor authentication is not enabled."
            );
        }

        // 3. Giải mã secret trong Backend.
        String secret = encryptionService.decrypt(
                userId,
                twoFactor.getEncryptedSecret()
        );

        // 4. Tìm chu kỳ tương ứng với OTP.
        OptionalLong matchingCounter =
                totpService.findMatchingCounter(
                        secret,
                        otpCode
                );

        Long lastAcceptedStep =
                twoFactor.getLastAcceptedStep();

        boolean invalidOtp = matchingCounter.isEmpty();

        boolean reusedOtp =
                matchingCounter.isPresent()
                        && lastAcceptedStep != null
                        && matchingCounter.getAsLong()
                        <= lastAcceptedStep;

        // 5. Xử lý mã sai, hết hạn hoặc đã sử dụng.
        if (invalidOtp || reusedOtp) {

            pendingLoginService.recordInvalidOtp(
                    request,
                    PendingLoginService.Step.VERIFY_OTP
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid or expired verification code."
            );
        }

        // 6. Ghi nhận chu kỳ OTP đã được sử dụng.
        twoFactor.setLastAcceptedStep(
                matchingCounter.getAsLong()
        );

        userTwoFactorRepository.saveAndFlush(twoFactor);

        /*
         * Thành công:
         * - OTP đã được xác minh.
         * - Chu kỳ OTP đã được lưu để chống sử dụng lại.
         *
         * Không tạo SecurityContext trong Service này.
         *
         * Service hoàn tất đăng nhập sẽ được gọi
         * sau khi verify() thành công.
         */
    }
}
