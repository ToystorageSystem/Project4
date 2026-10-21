
package com.toystorage.backend.services.businessmanager;

import com.toystorage.backend.dto.response.businessmanager.BusinessTransferDetailResponse;
import com.toystorage.backend.dto.response.businessmanager.BusinessTransferItemResponse;
import com.toystorage.backend.dto.response.businessmanager.BusinessTransferListResponse;
import com.toystorage.backend.entity.transfers.StockTransfer;
import com.toystorage.backend.entity.transfers.StockTransferItems;
import com.toystorage.backend.enums.transfers.TransferStatus;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.repository.businessmanager.BusinessTransferRepository;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessTransferService {

    private final BusinessTransferRepository repository;

    public Page<BusinessTransferListResponse> getTransfers(
            String keyword,
            TransferStatus status,
            int page,
            int size
    ) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page must be greater than or equal to 0"
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Size must be between 1 and 100"
            );
        }

        String normalizedKeyword =
                keyword == null || keyword.isBlank()
                        ? null
                        : keyword.trim();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        return repository.searchTransfers(
                normalizedKeyword,
                status,
                pageable
        ).map(this::toListResponse);
    }

    public BusinessTransferDetailResponse getTransferDetail(
            Long id
    ) {
        StockTransfer transfer = repository.findDetailById(id)
                .orElseThrow(() -> new NotFound(
                        "Stock transfer not found: " + id
                ));

        List<BusinessTransferItemResponse> items =
                transfer.getItems()
                        .stream()
                        .map(this::toItemResponse)
                        .toList();

        return BusinessTransferDetailResponse.builder()
                .id(transfer.getId())
                .transferCode(transfer.getTransferCode())
                .stockTransfersCode(
                        transfer.getStockTransfersCode()
                )
                .fromWarehouseId(
                        transfer.getFromWarehouse().getId()
                )
                .fromWarehouseName(
                        transfer.getFromWarehouse().getName()
                )
                .toWarehouseId(
                        transfer.getToWarehouse().getId()
                )
                .toWarehouseName(
                        transfer.getToWarehouse().getName()
                )
                .transferType(transfer.getTransferType())
                .status(transfer.getStatus())
                .reasonCode(
                        transfer.getReasonCode() == null
                                ? null
                                : transfer.getReasonCode().name()
                )
                .reasonNote(transfer.getReasonNote())
                .notes(transfer.getNotes())
                .rejectionReason(transfer.getRejectionReason())
                .createdById(
                        transfer.getCreatedBy().getId()
                )
                .createdByName(
                        transfer.getCreatedBy().getName()
                )
                .confirmedById(
                        transfer.getConfirmedBy() == null
                                ? null
                                : transfer.getConfirmedBy().getId()
                )
                .confirmedByName(
                        transfer.getConfirmedBy() == null
                                ? null
                                : transfer.getConfirmedBy().getName()
                )
                .createdAt(transfer.getCreatedAt())
                .confirmedAt(transfer.getConfirmedAt())
                .packingCompletedAt(
                        transfer.getPackingCompletedAt()
                )
                .shippedAt(transfer.getShippedAt())
                .receivedAt(transfer.getReceivedAt())
                .updatedAt(transfer.getUpdatedAt())
                .items(items)
                .build();
    }

    private BusinessTransferListResponse toListResponse(
            StockTransfer transfer
    ) {
        return BusinessTransferListResponse.builder()
                .id(transfer.getId())
                .transferCode(transfer.getTransferCode())
                .fromWarehouseId(
                        transfer.getFromWarehouse().getId()
                )
                .fromWarehouseName(
                        transfer.getFromWarehouse().getName()
                )
                .toWarehouseId(
                        transfer.getToWarehouse().getId()
                )
                .toWarehouseName(
                        transfer.getToWarehouse().getName()
                )
                .transferType(transfer.getTransferType())
                .status(transfer.getStatus())
                .createdAt(transfer.getCreatedAt())
                .shippedAt(transfer.getShippedAt())
                .receivedAt(transfer.getReceivedAt())
                .build();
    }

    private BusinessTransferItemResponse toItemResponse(
            StockTransferItems item
    ) {
        return BusinessTransferItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productCode(
                        item.getProduct().getProductsCode()
                )
                .productName(item.getProduct().getName())
                .requestedQuantity(item.getRequestedQuantity())
                .approvedQuantity(item.getApprovedQuantity())
                .pickedQuantity(item.getPickedQuantity())
                .packedQuantity(item.getPackedQuantity())
                .shippedQuantity(item.getShippedQuantity())
                .receivedQuantity(item.getReceivedQuantity())
                .shortageQuantity(item.getShortageQuantity())
                .surplusQuantity(item.getSurplusQuantity())
                .build();
    }
}
