
package com.toystorage.backend.repository.users;

import com.toystorage.backend.entity.users.UserTwoFactor;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserTwoFactorRepository
        extends JpaRepository<UserTwoFactor, Long> {

    /*
     * Lấy cấu hình 2FA của một tài khoản.
     *
     * Có thể sử dụng findById(userId)
     * được kế thừa từ JpaRepository.
     */

    /*
     * Khóa bản ghi khi xác minh OTP.
     *
     * Sử dụng trong @Transactional để tránh
     * hai yêu cầu đồng thời cùng chấp nhận
     * một mã OTP đã sử dụng.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT t
            FROM UserTwoFactor t
            WHERE t.userId = :userId
            """)
    Optional<UserTwoFactor> findByUserIdForUpdate(
            @Param("userId") Long userId
    );
}
