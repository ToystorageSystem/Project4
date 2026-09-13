package com.toystorage.backend.services.receipts.receiving;

import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.receipts.ReceiptInspections;
import com.toystorage.backend.entity.warehouses.WarehouseTaskClaim;
import com.toystorage.backend.enums.warehouses.WarehouseTaskType;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.repository.receipts.receiving.GoodsReceiptItemRepository;
import com.toystorage.backend.repository.receipts.receiving.GoodsReceiptRepository;
import com.toystorage.backend.repository.receipts.receiving.ReceiptInspectionRepository;
import com.toystorage.backend.services.warehouses.taskclaim.WarehouseTaskClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceivingMonitorQueryService {

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final GoodsReceiptItemRepository goodsReceiptItemRepository;
    private final ReceiptInspectionRepository receiptInspectionRepository;
    private final WarehouseTaskClaimService warehouseTaskClaimService;

    @Transactional(readOnly = true)
    public GoodsReceipts getGoodsReceipt(
            Long receiptId
    ) {

        return goodsReceiptRepository
                .findById(receiptId)
                .orElseThrow(() ->
                        new NotFound(
                                "Goods receipt not found with id: "
                                        + receiptId
                        )
                );
    }

    @Transactional(readOnly = true)
    public List<GoodsReceiptItems> getItems(
            Long receiptId
    ) {

        return goodsReceiptItemRepository
                .findByGoodsReceiptId(receiptId);
    }

    @Transactional(readOnly = true)
    public WarehouseTaskClaim resolveReceivingClaim(
            GoodsReceipts receipt
    ) {

        WarehouseTaskClaim activeClaim =
                warehouseTaskClaimService
                        .getActiveClaim(
                                WarehouseTaskType.GOODS_RECEIVING,
                                receipt.getId()
                        );

        if (activeClaim != null) {
            return activeClaim;
        }

        return warehouseTaskClaimService
                .getLatestClaim(
                        WarehouseTaskType.GOODS_RECEIVING,
                        receipt.getId()
                );
    }

    @Transactional(readOnly = true)
    public List<ReceiptInspections> getInspectionsForClaim(
            WarehouseTaskClaim claim
    ) {

        if (claim == null) {
            return List.of();
        }

        return receiptInspectionRepository
                .findByTaskClaimId(
                        claim.getId()
                );
    }
}
