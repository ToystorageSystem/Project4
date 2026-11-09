package com.toystorage.backend.repository.auth;
import com.toystorage.backend.entity.users.Users;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface AuthUserLockRepository extends Repository<Users, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Users u where u.id = :id")
    Optional<Users> lockById(@Param("id") Long id);
}
