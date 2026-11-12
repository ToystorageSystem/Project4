
package com.toystorage.backend.services.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.Serializable;

@Service
public class PendingLoginService {

    private static final String SESSION_KEY =
            PendingLoginService.class.getName() + ".PENDING_LOGIN";

    private static final int TIMEOUT_SECONDS = 10 * 60;

    private static final int MAX_FAILED_OTP_ATTEMPTS = 5;

    // Các bước đăng nhập chưa hoàn tất.
    public enum Step {
        CHANGE_PASSWORD,
        SETUP_2FA,
        VERIFY_OTP
    }

    // Chỉ lưu ID và trạng thái, không lưu mật khẩu hoặc mã OTP.
    public record PendingLogin(
            Long userId,
            Step step,
            long expiresAt,
            int failedOtpAttempts
    ) implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    // Bắt đầu phiên tạm sau khi xác thực mật khẩu.
    public PendingLogin begin(
            HttpServletRequest request,
            Long userId,
            Step firstStep
    ) {
        if (userId == null || userId <= 0 || firstStep == null) {
            throw new IllegalArgumentException(
                    "Invalid pending login information"
            );
        }

        // Hủy phiên cũ để không giữ quyền truy cập trước đó.
        HttpSession oldSession = request.getSession(false);

        if (oldSession != null) {
            oldSession.invalidate();
        }

        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval(TIMEOUT_SECONDS);

        PendingLogin pending = new PendingLogin(
                userId,
                firstStep,
                System.currentTimeMillis() + TIMEOUT_SECONDS * 1000L,
                0
        );

        session.setAttribute(SESSION_KEY, pending);

        return pending;
    }

    // Lấy phiên tạm và kiểm tra thời hạn.
    public PendingLogin current(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            throw unauthorized();
        }

        Object value = session.getAttribute(SESSION_KEY);

        if (!(value instanceof PendingLogin pending)) {
            throw unauthorized();
        }

        if (System.currentTimeMillis() >= pending.expiresAt()) {
            session.invalidate();
            SecurityContextHolder.clearContext();

            throw unauthorized();
        }

        return pending;
    }

    // Chỉ cho phép xử lý đúng bước hiện tại.
    public PendingLogin requireStep(
            HttpServletRequest request,
            Step expectedStep
    ) {
        PendingLogin pending = current(request);

        if (pending.step() != expectedStep) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Invalid authentication step"
            );
        }

        return pending;
    }

    // Chuyển bước sau khi Backend xác nhận thành công.
    public PendingLogin moveToStep(
            HttpServletRequest request,
            Step expectedStep,
            Step nextStep
    ) {
        PendingLogin pending = requireStep(
                request,
                expectedStep
        );

        if (nextStep == null) {
            throw new IllegalArgumentException(
                    "Next authentication step is required"
            );
        }

        PendingLogin updated = new PendingLogin(
                pending.userId(),
                nextStep,
                System.currentTimeMillis() + TIMEOUT_SECONDS * 1000L,
                0
        );

        request.getSession(false).setAttribute(
                SESSION_KEY,
                updated
        );

        return updated;
    }

    // Ghi nhận mã OTP sai và giới hạn số lần thử.
    public void recordInvalidOtp(
            HttpServletRequest request,
            Step expectedStep
    ) {
        PendingLogin pending = requireStep(
                request,
                expectedStep
        );

        HttpSession session = request.getSession(false);
        int failedAttempts = pending.failedOtpAttempts() + 1;

        if (failedAttempts >= MAX_FAILED_OTP_ATTEMPTS) {
            session.invalidate();
            SecurityContextHolder.clearContext();

            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many verification attempts. Sign in again."
            );
        }

        PendingLogin updated = new PendingLogin(
                pending.userId(),
                pending.step(),
                pending.expiresAt(),
                failedAttempts
        );

        session.setAttribute(SESSION_KEY, updated);
    }

    // Chỉ xóa trạng thái tạm sau khi hoàn tất xác minh.
    // Việc tạo SecurityContext đầy đủ được thực hiện ở bước sau.
    public void clearPending(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.removeAttribute(SESSION_KEY);
        }
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Authentication session expired. Sign in again."
        );
    }
}
