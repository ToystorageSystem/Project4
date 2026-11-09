package com.toystorage.backend.repository.auth;
import com.toystorage.backend.entity.auth.PasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String hash);
    long countByUserIdAndCreatedAtAfter(Long userId, Instant since);
    Optional<PasswordResetToken> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PasswordResetToken t where t.id = :id")
    Optional<PasswordResetToken> lockById(@Param("id") Long id);
    @Modifying
    @Query("update PasswordResetToken t set t.usedAt = :now where t.userId = :userId and t.usedAt is null")
    int invalidateAll(@Param("userId") Long userId, @Param("now") Instant now);
}
