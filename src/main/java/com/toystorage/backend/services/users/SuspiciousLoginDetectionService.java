
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

    // Nguong phat hien: 5 lan dang nhap sai trong 5 phut.
    private static final int FAILED_LOGIN_THRESHOLD = 5;
    private static final int TIME_WINDOW_MINUTES = 5;

    // Diem rui ro minh hoa, chua phai thuat toan danh gia.
    private static final BigDecimal RISK_SCORE =
            new BigDecimal("50.00");

    // Noi dung canh bao hien thi cho nguoi dung bang tieng Anh.
    private static final String ALERT_DESCRIPTION_PREFIX =
            "Suspicious login activity:";

    // Tien to ma canh bao, dung de kiem tra trung lap.
    private static final String ALERT_CODE_PREFIX =
            "SAA-LOGIN-";

    private final EntityManager entityManager;

    private final SuspiciousActivityAlertRepository
            suspiciousActivityAlertRepository;

    /*
     * Kiem tra dang nhap that bai bat thuong.
     *
     * Neu cung mot tai khoan dang nhap sai it nhat
     * 5 lan trong 5 phut thi tao canh bao NEW.
     *
     * REQUIRES_NEW giup canh bao duoc luu doc lap,
     * ngay ca khi qua trinh Login tra ve 401.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void detectAfterFailedLogin(ActivityLogs failedLog) {

        // =====================================================
        // 1. VALIDATE FAILED LOGIN
        // =====================================================

        // Chi tao canh bao neu tai khoan thuc su ton tai.
        if (failedLog == null
                || failedLog.getId() == null
                || failedLog.getUser() == null
                || failedLog.getUser().getId() == null) {
            return;
        }

        Long userId = failedLog.getUser().getId();

        // =====================================================
        // 2. LOCK USER
        // =====================================================

        /*
         * Khoa ban ghi nguoi dung trong transaction.
         * Han che tao canh bao trung khi nhieu request
         * dang nhap sai xay ra dong thoi.
         */
        Users user = entityManager.find(
                Users.class,
                userId,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (user == null) {
            return;
        }

        // =====================================================
        // 3. TIME WINDOW
        // =====================================================

        LocalDateTime since = LocalDateTime.now()
                .minusMinutes(TIME_WINDOW_MINUTES);

        // =====================================================
        // 4. COUNT FAILED LOGIN ATTEMPTS
        // =====================================================

        /*
         * Dem cac ban ghi LOGIN_FAILED cua cung tai khoan
         * trong khoang 5 phut gan nhat.
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
                .setParameter(
                        "since",
                        Timestamp.valueOf(since)
                )
                .getSingleResult();

        long totalFailed = failedCount.longValue();

        // Chua du nguong thi khong tao canh bao.
        if (totalFailed < FAILED_LOGIN_THRESHOLD) {
            return;
        }

        // =====================================================
        // 5. CHECK DUPLICATE ALERT
        // =====================================================

        /*
         * Kiem tra canh bao tu dong da duoc tao chua.
         *
         * Su dung ma SAA-LOGIN- thay vi description.
         * Nhu vay canh bao cu bang tieng Viet hoac
         * tieng Anh deu duoc tinh de tranh trung lap.
         *
         * Khong phu thuoc trang thai:
         * NEW, INVESTIGATING, RESOLVED, FALSE_POSITIVE.
         */
        Number existingCount = (Number) entityManager
                .createNativeQuery("""
                        SELECT COUNT(*)
                        FROM suspicious_activity_alerts
                        WHERE user_id = :userId
                          AND suspicious_activity_alerts_code
                              LIKE 'SAA-LOGIN-%'
                          AND detected_at >= :since
                        """)
                .setParameter("userId", userId)
                .setParameter(
                        "since",
                        Timestamp.valueOf(since)
                )
                .getSingleResult();

        // Neu da co canh bao trong khoang thoi gian nay,
        // khong tao them canh bao trung.
        if (existingCount.longValue() > 0) {
            return;
        }

        // =====================================================
        // 6. CREATE SECURITY ALERT
        // =====================================================

        /*
         * Tao noi dung canh bao bang tieng Anh.
         *
         * Vi du:
         * Suspicious login activity: 5 failed login attempts
         * within 5 minutes. Account: DEV002
         */
        String description =
                ALERT_DESCRIPTION_PREFIX
                + " "
                + totalFailed
                + " failed login attempts within "
                + TIME_WINDOW_MINUTES
                + " minutes. Account: "
                + user.getUserCode();

        SuspiciousActivityAlerts alert =
                SuspiciousActivityAlerts.builder()

                        .user(user)

                        .riskScore(RISK_SCORE)

                        .description(description)

                        // Lien ket voi Activity Log that bai vua tao.
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

        // =====================================================
        // 7. SAVE ALERT TO MYSQL
        // =====================================================

        suspiciousActivityAlertRepository.saveAndFlush(alert);
    }

    // =========================================================
    // GENERATE ALERT CODE
    // =========================================================

    // Sinh ma canh bao duy nhat, do dai khong qua 50 ky tu.
    private String generateAlertCode() {

        return ALERT_CODE_PREFIX
                + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 24)
                        .toUpperCase(Locale.ROOT);
    }
}
