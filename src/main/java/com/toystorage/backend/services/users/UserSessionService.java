package com.toystorage.backend.services.users;

import com.toystorage.backend.config.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserSessionService {

    private final SessionRegistry sessionRegistry;


    // =========================================================
    // EXPIRE ALL USER SESSIONS
    // =========================================================

    public int expireSessionsByUserId(
            Long userId
    ) {

        int expiredCount = 0;


        /*
         * SessionRegistry có thể chứa nhiều principal.
         */
        for (
                Object principal :
                sessionRegistry.getAllPrincipals()
        ) {

            /*
             * Login của project đang dùng
             * CustomUserDetails làm principal.
             */
            if (
                    principal
                            instanceof CustomUserDetails userDetails
            ) {

                /*
                 * Không phải user cần khóa
                 * thì bỏ qua.
                 */
                if (
                        !userId.equals(
                                userDetails.getId()
                        )
                ) {

                    continue;
                }


                /*
                 * Lấy tất cả session hiện còn hoạt động.
                 */
                List<SessionInformation> sessions =
                        sessionRegistry
                                .getAllSessions(
                                        principal,
                                        false
                                );


                for (
                        SessionInformation session :
                        sessions
                ) {

                    if (!session.isExpired()) {

                        /*
                         * Đánh dấu session hết hiệu lực.
                         *
                         * Request kế tiếp của session này
                         * sẽ bị Spring Security chặn.
                         */
                        session.expireNow();

                        expiredCount++;
                    }
                }
            }
        }


        return expiredCount;
    }
}