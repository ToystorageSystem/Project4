
package com.toystorage.backend.services.auth;

import com.toystorage.backend.dto.response.auth.TotpSetupResponse;
import com.toystorage.backend.entity.users.UserTwoFactor;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.repository.users.UserTwoFactorRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TwoFactorSetupService {

    private final PendingLoginService pendingLoginService;

    private final TotpService totpService;

    private final TotpSecretEncryptionService encryptionService;

    private final UserTwoFactorRepository userTwoFactorRepository;

    private final EntityManager entityManager;

    /*
     * Thiết lập Google Authenticator.
     *
     * Chỉ cho phép thực hiện khi tài khoản đã
     * xác thực mật khẩu và đang ở bước SETUP_2FA.
     *
     * Chưa cấp quyền đăng nhập đầy đủ tại đây.
     */
    @Transactional
    public TotpSetupResponse setup(HttpServletRequest request) {

        // =====================================================
        // 1. CHECK PENDING LOGIN
        // =====================================================

        PendingLoginService.PendingLogin pending =
                pendingLoginService.requireStep(
                        request,
                        PendingLoginService.Step.SETUP_2FA
                );

        Long userId = pending.userId();

        // =====================================================
        // 2. GET AND LOCK USER
        // =====================================================

        /*
         * Khóa bản ghi người dùng trong transaction
         * để hạn chế hai phiên cùng tạo cấu hình 2FA
         * cho một tài khoản.
         */
        Users user = entityManager.find(
                Users.class,
                userId,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication session is invalid. Sign in again."
            );
        }

        // =====================================================
        // 3. CHECK EXISTING 2FA CONFIGURATION
        // =====================================================

        UserTwoFactor twoFactor = userTwoFactorRepository
                .findById(userId)
                .orElse(null);

        /*
         * Nếu đã bật 2FA thì không cho phép
         * tạo lại hoặc tiết lộ secret hiện tại
         * thông qua chức năng Setup.
         */
        if (twoFactor != null && twoFactor.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Two-factor authentication is already enabled."
            );
        }

        // =====================================================
        // 4. PREPARE TOTP SECRET
        // =====================================================

        String secret;

        if (twoFactor == null) {

            /*
             * Chưa có cấu hình:
             * Tạo secret mới, mã hóa và lưu vào database.
             */
            secret = totpService.generateSecret();

            byte[] encryptedSecret = encryptionService.encrypt(
                    userId,
                    secret
            );

            twoFactor = UserTwoFactor.builder()
                    .userId(userId)
                    .encryptedSecret(encryptedSecret)
                    .enabled(false)
                    .lastAcceptedStep(null)
                    .build();

        } else if (twoFactor.getEncryptedSecret() == null
                || twoFactor.getEncryptedSecret().length == 0) {

            /*
             * Có bản ghi nhưng chưa có secret:
             * Tạo secret mới cho lần thiết lập này.
             */
            secret = totpService.generateSecret();

            twoFactor.setEncryptedSecret(
                    encryptionService.encrypt(userId, secret)
            );

            twoFactor.setEnabled(false);
            twoFactor.setLastAcceptedStep(null);

        } else {

            /*
             * Đã bắt đầu Setup nhưng chưa kích hoạt:
             * Dùng lại secret đang chờ xác nhận.
             *
             * Việc này giúp tải lại màn hình Setup
             * không làm thay đổi QR Code liên tục.
             */
            secret = encryptionService.decrypt(
                    userId,
                    twoFactor.getEncryptedSecret()
            );
        }

        // =====================================================
        // 5. SAVE PENDING 2FA CONFIGURATION
        // =====================================================

        /*
         * enabled vẫn là false.
         *
         * Chỉ khi người dùng nhập OTP hợp lệ
         * ở bước Confirm, Backend mới bật enabled=true.
         */
        twoFactor.setEnabled(false);

        twoFactor.setLastAcceptedStep(null);

        userTwoFactorRepository.saveAndFlush(twoFactor);

        // =====================================================
        // 6. GENERATE GOOGLE AUTHENTICATOR URI
        // =====================================================

        String otpAuthUri = totpService.createOtpAuthUri(
                user.getEmail(),
                secret
        );

        // =====================================================
        // 7. RETURN SETUP RESPONSE
        // =====================================================

        /*
         * URI và manualEntryKey chỉ được trả về
         * trong bước thiết lập 2FA hợp lệ.
         *
         * Không ghi secret, QR URI hoặc OTP vào log.
         */
        return TotpSetupResponse.builder()
                .otpAuthUri(otpAuthUri)
                .manualEntryKey(secret)
                .build();
    }
}
