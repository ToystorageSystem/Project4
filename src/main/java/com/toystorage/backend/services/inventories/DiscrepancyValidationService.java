package com.toystorage.backend.services.inventories;

import com.toystorage.backend.entity.inventories.DiscrepancyReports;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.inventories.DiscrepancyReferenceType;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Forbidden;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;
import com.toystorage.backend.repository.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DiscrepancyValidationService {

    private final UserRepository userRepository;

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

    public Long getWarehouseId(Users user) {

        if (user.getWarehouse() == null) {
            throw new Forbidden(
                    "User is not assigned to a warehouse"
            );
        }

        return user.getWarehouse().getId();
    }

    public void validateSameWarehouse(
            Users user,
            DiscrepancyReports report
    ) {

        Long warehouseId =
                getWarehouseId(user);

        if (report.getWarehouse() == null
                || !warehouseId.equals(
                report.getWarehouse().getId()
        )) {

            throw new Forbidden(
                    "You cannot handle discrepancy from another warehouse"
            );
        }
    }

    public void validateGoodsReceipt(
            DiscrepancyReports report
    ) {

        if (report.getReferenceType()
                != DiscrepancyReferenceType.GOODS_RECEIPT) {

            throw new BadRequest(
                    "This discrepancy is not related to a goods receipt"
            );
        }
    }
}