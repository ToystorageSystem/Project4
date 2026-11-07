
package com.toystorage.backend.services.auth;

import com.toystorage.backend.dto.request.auth.ChangePasswordRequest;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.repository.users.UserTwoFactorRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PendingPasswordChangeService {

    private final PendingLoginService pendingLoginService;

    private final UserTwoFactorRepository userTwoFactorRepository;

    private final PasswordEncoder passwordEncoder;

    private final EntityManager entityManager;

    /*
     * Doi mat khau bat buoc trong phien dang nhap tam.
     *
     * Chi cho phep tai khoan dang o buoc CHANGE_PASSWORD.
     * Chua cap SecurityContext hoac quyen truy cap he thong.
     */
    @Transactional
    public String changeRequiredPassword(
            HttpServletRequest httpRequest,
            ChangePasswordRequest request
    ) {

        // =====================================================
        // 1. CHECK PENDING LOGIN
        // =====================================================

        PendingLoginService.PendingLogin pending =
                pendingLoginService.requireStep(
                        httpRequest,
                        PendingLoginService.Step.CHANGE_PASSWORD
                );

        Long userId = pending.userId();

        // =====================================================
        // 2. GET AND LOCK USER
        // =====================================================

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
        // 3. CHECK PASSWORD CHANGE REQUIREMENT
        // =====================================================

        if (!Boolean.TRUE.equals(user.getMustChangePassword())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Password change is not required."
            );
        }

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password information is required."
            );
        }

        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();

        // =====================================================
        // 4. VALIDATE CURRENT PASSWORD
        // =====================================================

        if (oldPassword == null
                || oldPassword.isBlank()
                || !passwordEncoder.matches(
                        oldPassword,
                        user.getPassword()
                )) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Current password is incorrect."
            );
        }

        // =====================================================
        // 5. VALIDATE NEW PASSWORD
        // =====================================================

        if (newPassword == null
                || newPassword.isBlank()
                || newPassword.length() < 6) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "New password must contain at least 6 characters."
            );
        }

        // Khong cho phep dung lai mat khau hien tai.
        if (passwordEncoder.matches(
                newPassword,
                user.getPassword()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "New password must differ from the current password."
            );
        }

        // =====================================================
        // 6. UPDATE PASSWORD
        // =====================================================

        /*
         * Ma hoa mat khau moi truoc khi luu.
         * Khong luu hoac ghi log mat khau dang ro.
         */
        user.setPassword(
                passwordEncoder.encode(newPassword)
        );

        user.setMustChangePassword(false);

        entityManager.flush();

        // =====================================================
        // 7. CHECK TWO-FACTOR AUTHENTICATION
        // =====================================================

        boolean enabledTwoFactor = userTwoFactorRepository
                .findById(userId)
                .map(twoFactor -> twoFactor.isEnabled())
                .orElse(false);

        PendingLoginService.Step nextStep;

        String nextStatus;

        if (enabledTwoFactor) {

            // Tai khoan da bat Google Authenticator.
            nextStep = PendingLoginService.Step.VERIFY_OTP;

            nextStatus = "REQUIRE_2FA_VERIFICATION";

        } else {

            // Tai khoan chua thiet lap Google Authenticator.
            nextStep = PendingLoginService.Step.SETUP_2FA;

            nextStatus = "REQUIRE_2FA_SETUP";
        }

        // =====================================================
        // 8. MOVE TO NEXT STEP AFTER COMMIT
        // =====================================================

        /*
         * Chi chuyen buoc sau khi transaction commit.
         *
         * Neu luu mat khau that bai va rollback,
         * PendingLogin van o buoc CHANGE_PASSWORD.
         */
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {

                        pendingLoginService.moveToStep(
                                httpRequest,
                                PendingLoginService.Step.CHANGE_PASSWORD,
                                nextStep
                        );
                    }
                }
        );

        // =====================================================
        // 9. RETURN NEXT AUTHENTICATION STATUS
        // =====================================================

        /*
         * Chua tao SecurityContext day du.
         * Nguoi dung van phai hoan thanh 2FA.
         */
        return nextStatus;
    }
}
