package com.toystorage.backend.repository.users;

import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.users.UserStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository
        extends JpaRepository<Users, Long> {

    /*
     * LOGIN đang dùng.
     * KHÔNG XÓA.
     */
    Optional<Users> findByEmail(String email);


    /*
     * Kiểm tra email trùng khi tạo user.
     */
    boolean existsByEmailIgnoreCase(String email);


    /*
     * Kiểm tra email trùng khi cập nhật.
     *
     * Ví dụ đang sửa User ID = 5,
     * email của chính User 5 thì vẫn hợp lệ.
     */
    boolean existsByEmailIgnoreCaseAndIdNot(
            String email,
            Long id
    );


    /*
     * Kiểm tra CCCD trùng khi tạo.
     */
    boolean existsByIdentityCard(
            String identityCard
    );


    /*
     * Kiểm tra CCCD trùng khi cập nhật.
     */
    boolean existsByIdentityCardAndIdNot(
            String identityCard,
            Long id
    );


    /*
     * Sinh mã user tự động.
     *
     * Ví dụ DB đang có:
     *
     * USR001
     * USR002
     * USR011
     *
     * Query trả về 11.
     *
     * Service sẽ sinh:
     * USR012
     */
    @Query(
            value = """
                    SELECT COALESCE(
                        MAX(
                            CAST(
                                SUBSTRING(user_code, 4)
                                AS UNSIGNED
                            )
                        ),
                        0
                    )
                    FROM users
                    WHERE user_code LIKE 'USR%'
                    """,
            nativeQuery = true
    )
    Long findMaxUserCodeNumber();


    /*
     * Danh sách + tìm kiếm + lọc.
     */
    @Query("""
        SELECT DISTINCT u
        FROM Users u
        WHERE

            (
                :keyword IS NULL
                OR :keyword = ''
                OR LOWER(u.name)
                    LIKE LOWER(
                        CONCAT('%', :keyword, '%')
                    )
                OR LOWER(u.email)
                    LIKE LOWER(
                        CONCAT('%', :keyword, '%')
                    )
                OR LOWER(u.userCode)
                    LIKE LOWER(
                        CONCAT('%', :keyword, '%')
                    )
            )

            AND (
                :status IS NULL
                OR u.status = :status
            )

            AND (
                :warehouseId IS NULL
                OR u.warehouse.id = :warehouseId
            )

            AND (
                :roleId IS NULL

                OR EXISTS (
                    SELECT ur.id
                    FROM UserRoles ur
                    WHERE ur.user.id = u.id
                    AND ur.role.id = :roleId
                )
            )
        """)
    Page<Users> searchUsers(
            @Param("keyword") String keyword,

            @Param("roleId")
            Long roleId,

            @Param("warehouseId")
            Long warehouseId,

            @Param("status")
            UserStatus status,

            Pageable pageable
    );
}