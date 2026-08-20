package com.toystorage.backend.services.warehouses.damagedgoods;

import com.toystorage.backend.entity.warehouses.DamagedGoodsReports;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;
import com.toystorage.backend.repository.warehouses.DamagedGoodsReportRepository;
import com.toystorage.backend.repository.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class DamagedGoodsValidationService {

    private final UserRepository userRepository;

    private final DamagedGoodsReportRepository
            damagedGoodsReportRepository;

    public Users getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                authentication.getPrincipal()
        )) {

            throw new Unauthorized(
                    "User is not authenticated"
            );
        }

        return userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new NotFound(
                                "Authenticated user not found"
                        )
                );

    }


    public DamagedGoodsReports getReport(
            Long reportId
    ) {

        return damagedGoodsReportRepository
                .findById(reportId)
                .orElseThrow(() ->
                        new NotFound(
                                "Damaged goods report not found: "
                                        + reportId
                        )
                );
    }


    public void validateWarehouse(
            Users manager,
            DamagedGoodsReports report
    ) {

        if (manager.getWarehouse() == null) {

            throw new Forbidden(
                    "Manager is not assigned to a warehouse"
            );
        }

        if (!manager.getWarehouse()
                .getId()
                .equals(
                        report.getWarehouse().getId()
                )) {

            throw new Forbidden(
                    "You cannot handle damaged goods "
                            + "from another warehouse"
            );
        }
    }
    public Long getWarehouseId(
            Users user
    ) {

        if (user.getWarehouse() == null) {
            throw new Forbidden(
                    "User is not assigned to a warehouse"
            );
        }

        return user.getWarehouse().getId();
    }
}