
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
public class TwoFactorConfirmService {

    private final PendingLoginService pendingLoginService;

    private final UserTwoFactorRepository userTwoFactorRepository;

    private final TotpSecretEncryptionService encryptionService;

    private final TotpService totpService;

    /*
     * Xac nhan ma OTP trong lan thiet lap 2FA dau tien.
     *
     * Chi cho phep thuc hien khi phien dang nhap
     * dang o buoc SETUP_2FA.
     *
     * Thanh cong:
     * - Kich hoat 2FA.
     * - Ghi nhan chu ky OTP da su dung.
     * - Khong cap SecurityContext tai day.
     */
    @Transactional
    public void confirm(
            HttpServletRequest request,
            String otpCode
    ) {

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
        // 2. LOCK TWO-FACTOR CONFIGURATION
        // =====================================================

        /*
         * Khoa ban ghi de tranh hai request dong thoi
         * cung chap nhan mot ma OTP.
         */
        UserTwoFactor twoFactor = userTwoFactorRepository
                .findByUserIdForUpdate(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Two-factor authentication setup has not been started."
                        )
                );

        if (twoFactor.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Two-factor authentication is already enabled."
            );
        }

        byte[] encryptedSecret = twoFactor.getEncryptedSecret();

        if (encryptedSecret == null || encryptedSecret.length == 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Two-factor authentication setup has not been started."
            );
        }

        // =====================================================
        // 3. DECRYPT SECRET
        // =====================================================

        /*
         * Secret chi duoc giai ma trong Backend.
         * Khong ghi secret hoac OTP vao log.
         */
        String secret = encryptionService.decrypt(
                userId,
                encryptedSecret
        );

        // =====================================================
        // 4. VERIFY OTP
        // =====================================================

        /*
         * findMatchingCounter tra ve chu ky
         * 30 giay neu OTP hop le.
         */
        OptionalLong matchingCounter =
                totpService.findMatchingCounter(
                        secret,
                        otpCode
                );

        Long lastAcceptedStep =
                twoFactor.getLastAcceptedStep();

        boolean invalidCode = matchingCounter.isEmpty();

        boolean reusedCode =
                matchingCounter.isPresent()
                        && lastAcceptedStep != null
                        && matchingCounter.getAsLong()
                        <= lastAcceptedStep;

        if (invalidCode || reusedCode) {

            /*
             * Ghi nhan so lan OTP sai trong phien tam.
             * Du 5 lan thi huy phien.
             */
            pendingLoginService.recordInvalidOtp(
                    request,
                    PendingLoginService.Step.SETUP_2FA
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid or expired verification code."
            );
        }

        // =====================================================
        // 5. ENABLE TWO-FACTOR AUTHENTICATION
        // =====================================================

        /*
         * Chi kich hoat khi OTP da duoc xac minh.
         *
         * Luu counter de ngan su dung lai
         * ma OTP trong chu ky da chap nhan.
         */
        twoFactor.setEnabled(true);

        twoFactor.setLastAcceptedStep(
                matchingCounter.getAsLong()
        );

        userTwoFactorRepository.saveAndFlush(twoFactor);

        /*
         * KHONG xoa PendingLogin o day.
         *
         * Controller sau nay se goi Service hoan tat
         * dang nhap ngay sau khi confirm thanh cong,
         * trong cung request.
         *
         * Chi Service hoan tat dang nhap moi duoc
         * tao SecurityContext co quyen truy cap.
         */
    }
}
