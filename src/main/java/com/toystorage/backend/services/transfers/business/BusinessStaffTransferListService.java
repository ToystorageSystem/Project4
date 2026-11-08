
package com.toystorage.backend.services.transfers.business;

import com.toystorage.backend.dto.response.transfers.StockTransferListResponse;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.transfers.TransferStatus;
import com.toystorage.backend.enums.transfers.TransferType;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;
import com.toystorage.backend.repository.transfers.picking.StockTransferRepository;
import com.toystorage.backend.repository.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessStaffTransferListService {

    private final StockTransferRepository stockTransferRepository;
    private final UserRepository userRepository;

    // Lay danh sach phieu dieu chuyen do nguoi dung hien tai tao.
    @Transactional(readOnly = true)
    public Page<StockTransferListResponse> getMyTransfers(
            String keyword,
            TransferStatus status,
            int page,
            int size
    ) {
        // Kiem tra tham so phan trang.
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequest(
                    "Page must be >= 0 and size must be between 1 and 100"
            );
        }

        // Chuan hoa tu khoa tim kiem.
        String search = keyword == null ? null : keyword.trim();

        if (search != null && search.isEmpty()) {
            search = null;
        }

        if (search != null && search.length() > 100) {
            throw new BadRequest(
                    "Keyword must not exceed 100 characters"
            );
        }

        // Xac dinh nguoi dung dang dang nhap.
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new Unauthorized("User is not authenticated");
        }

        Users currentUser = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new NotFound("Authenticated user not found")
                );

        // Sap xep phieu moi nhat len dau.
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        // Chi lay phieu NORMAL do chinh nguoi dung tao.
        return stockTransferRepository.findMyNormalTransfers(
                currentUser.getId(),
                TransferType.NORMAL,
                search,
                status,
                pageable
        ).map(this::toResponse);
    }

    // Chuyen Entity thanh DTO tra ve Frontend.
    private StockTransferListResponse toResponse(
            StockTransfer transfer
    ) {
        return StockTransferListResponse.builder()
                .id(transfer.getId())
                .transferCode(transfer.getTransferCode())
                .status(transfer.getStatus())
                .fromWarehouseName(
                        transfer.getFromWarehouse().getName()
                )
                .toWarehouseName(
                        transfer.getToWarehouse().getName()
                )
                .expectedShipmentDate(
                        transfer.getExpectedShipmentDate()
                )
                .expectedReceiptDate(
                        transfer.getExpectedReceiptDate()
                )
                .createdAt(transfer.getCreatedAt())
                .updatedAt(transfer.getUpdatedAt())
                .build();
    }
}
