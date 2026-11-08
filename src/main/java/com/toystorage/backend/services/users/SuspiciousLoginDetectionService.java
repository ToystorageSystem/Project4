
package com.toystorage.backend.services.users;

import com.toystorage.backend.entity.users.ActivityLogs;
import com.toystorage.backend.entity.users.SuspiciousActivityAlerts;
import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.users.SuspiciousAlertLevel;
import com.toystorage.backend.enums.users.SuspiciousAlertStatus;

import com.toystorage.backend.repository.users.SuspiciousActivityAlertRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SuspiciousLoginDetectionService {

    // Ngưỡng đề xuất để kiểm thử cảnh báo.
    private static final int FAILED_LOGIN_THRESHOLD = 5;
    private static final int TIME_WINDOW_MINUTES = 5;

    // Điểm rủi ro minh họa, chưa phải thuật toán chấm điểm.
    private static final BigDecimal RISK_SCORE =
            new BigDecimal("50.00");

    private static final String ALERT_DESCRIPTION_PREFIX =
            "Phát hiện đăng nhập thất bại liên tục:";

    private final EntityManager entityManager;

    private final SuspiciousActivityAlertRepository
            suspiciousActivityAlertRepository;

    /*
     * Kiểm tra xem một lần đăng nhập thất bại
     * có làm vượt ngưỡng cảnh báo hay không.
     *
     * REQUIRES_NEW giúp cảnh báo được lưu riêng,
     * ngay cả khi quá trình Login sau đó trả 401.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void detectAfterFailedLogin(ActivityLogs failedLog) {

        // Chỉ xét tài khoản thực sự tồn tại.
        if (failedLog == null
                || failedLog.getId() == null
                || failedLog.getUser() == null
                || failedLog.getUser().getId() == null) {
            return;
        }

        Long userId = failedLog.getUser().getId();

        /*
         * Khóa bản ghi tài khoản trong transaction.
         * Giúp tuần tự hóa các lần kiểm tra cùng tài khoản
         * khi có nhiều yêu cầu đăng nhập đồng thời.
         */
        Users user = entityManager.find(
                Users.class,
                userId,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (user == null) {
            return;
        }

        LocalDateTime since = LocalDateTime.now()
                .minusMinutes(TIME_WINDOW_MINUTES);

        /*
         * Đếm các lần đăng nhập thất bại
         * của cùng tài khoản trong 5 phút.
         */
        Number failedCount = (Number) entityManager
                .createNativeQuery("""
                        SELECT COUNT(*)
                        FROM activity_logs
                        WHERE user_id = :userId
                          AND action = 'LOGIN_FAILED'
                          AND created_at >= :since
                        """)
                .setParameter("userId", userId)
                .setParameter("since", Timestamp.valueOf(since))
                .getSingleResult();

        long totalFailed = failedCount.longValue();

        if (totalFailed < FAILED_LOGIN_THRESHOLD) {
            return;
        }

        /*
         * Kiểm tra đã có cảnh báo tự động
         * của tài khoản này trong 5 phút chưa.
         *
         * Không phụ thuộc trạng thái cảnh báo:
         * NEW, INVESTIGATING hoặc RESOLVED.
         */
        Number existingCount = (Number) entityManager
                .createNativeQuery("""
                        SELECT COUNT(*)
                        FROM suspicious_activity_alerts
                        WHERE user_id = :userId
                          AND description LIKE :description
                          AND detected_at >= :since
                        """)
                .setParameter("userId", userId)
                .setParameter(
                        "description",
                        ALERT_DESCRIPTION_PREFIX + "%"
                )
                .setParameter("since", Timestamp.valueOf(since))
                .getSingleResult();

        if (existingCount.longValue() > 0) {
            return;
        }

        /*
         * Tạo cảnh báo mới.
         *
         * relatedLog trỏ tới lần LOGIN_FAILED
         * vừa được ActivityLogService ghi nhận.
         */
        SuspiciousActivityAlerts alert =
                SuspiciousActivityAlerts.builder()
                        .user(user)
                        .riskScore(RISK_SCORE)
                        .description(
                                ALERT_DESCRIPTION_PREFIX
                                + " "
                                + totalFailed
                                + " lần trong "
                                + TIME_WINDOW_MINUTES
                                + " phút. Tài khoản: "
                                + user.getUserCode()
                        )
                        .relatedLog(
                                entityManager.getReference(
                                        ActivityLogs.class,
                                        failedLog.getId()
                                )
                        )
                        .status(SuspiciousAlertStatus.NEW)
                        .alertLevel(SuspiciousAlertLevel.MEDIUM)
                        .suspiciousActivityAlertCode(
                                generateAlertCode()
                        )
                        .build();

        suspiciousActivityAlertRepository
                .saveAndFlush(alert);
    }

    // Sinh mã cảnh báo riêng, ngắn hơn 50 ký tự.
    private String generateAlertCode() {
        return "SAA-LOGIN-"
                + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 24)
                        .toUpperCase(Locale.ROOT);
    }
}
