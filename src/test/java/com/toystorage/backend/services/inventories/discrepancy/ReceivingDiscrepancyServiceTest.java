
package com.toystorage.backend.services.inventories.discrepancy;

import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.receipts.ReceiptInspections;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.WarehouseTaskClaim;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.mapper.inventories.discrepancy.DiscrepancyReportMapper;
import com.toystorage.backend.repository.inventories.discrepancy.DiscrepancyItemRepository;
import com.toystorage.backend.repository.inventories.discrepancy.DiscrepancyReportRepository;
import com.toystorage.backend.repository.receipts.receiving.GoodsReceiptItemRepository;
import com.toystorage.backend.repository.receipts.receiving.ReceiptInspectionRepository;
import com.toystorage.backend.services.receipts.incidents.ReceivingIncidentReportService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceivingDiscrepancyServiceTest {

    @Mock
    private DiscrepancyReportRepository discrepancyReportRepository;

    @Mock
    private DiscrepancyItemRepository discrepancyItemRepository;

    @Mock
    private ReceiptInspectionRepository receiptInspectionRepository;

    @Mock
    private DiscrepancyReportMapper discrepancyReportMapper;

    @Mock
    private DiscrepancyValidationService validationService;

    @Mock
    private DiscrepancyResolutionService resolutionService;

    @Mock
    private GoodsReceiptItemRepository goodsReceiptItemRepository;

    @Mock
    private ReceivingIncidentReportService receivingIncidentReportService;

    @InjectMocks
    private ReceivingDiscrepancyService service;

    // Test 1: Tu choi ket qua kiem dem cua phieu nhap khac.
    @Test
    void shouldRejectInspectionFromDifferentReceipt() {

        GoodsReceipts receipt = mock(GoodsReceipts.class);
        WarehouseTaskClaim taskClaim = mock(WarehouseTaskClaim.class);
        ReceiptInspections inspection = mock(ReceiptInspections.class);
        Users staff = mock(Users.class);

        when(receipt.getId()).thenReturn(43L);
        when(taskClaim.getId()).thenReturn(1L);
        when(inspection.getId()).thenReturn(101L);

        // Phien kiem dem co du lieu.
        when(receiptInspectionRepository.findByTaskClaimId(1L))
                .thenReturn(List.of(inspection));

        // Nhung ket qua kiem dem khong thuoc phieu nhap 43.
        when(receiptInspectionRepository.findByGoodsReceiptId(43L))
                .thenReturn(List.of());

        BadRequest exception = assertThrows(
                BadRequest.class,
                () -> service.syncFromFinishedInspection(
                        receipt,
                        taskClaim,
                        staff
                )
        );

        assertTrue(
                exception.getMessage().contains(
                        "Inspection does not belong to goods receipt"
                )
        );

        // Khong duoc thay doi bao cao khi du lieu khong hop le.
        verifyNoInteractions(
                discrepancyReportRepository,
                discrepancyItemRepository
        );
    }

    // Test 2: San pham phai thuoc chi tiet phieu nhap.
    @Test
    void shouldRejectProductNotBelongingToReceipt() {

        GoodsReceipts receipt = mock(GoodsReceipts.class);
        WarehouseTaskClaim taskClaim = mock(WarehouseTaskClaim.class);

        ReceiptInspections inspection = mock(
                ReceiptInspections.class,
                RETURNS_DEEP_STUBS
        );

        Users staff = mock(Users.class);

        when(receipt.getId()).thenReturn(43L);
        when(taskClaim.getId()).thenReturn(2L);
        when(inspection.getId()).thenReturn(102L);
        when(inspection.getProduct().getId()).thenReturn(7L);

        // Ket qua kiem dem thuoc phieu nhap 43.
        when(receiptInspectionRepository.findByTaskClaimId(2L))
                .thenReturn(List.of(inspection));

        when(receiptInspectionRepository.findByGoodsReceiptId(43L))
                .thenReturn(List.of(inspection));

        // Nhung san pham 7 khong thuoc phieu nhap 43.
        when(
                goodsReceiptItemRepository
                        .findByGoodsReceiptIdAndProductId(43L, 7L)
        ).thenReturn(Optional.empty());

        BadRequest exception = assertThrows(
                BadRequest.class,
                () -> service.syncFromFinishedInspection(
                        receipt,
                        taskClaim,
                        staff
                )
        );

        assertTrue(
                exception.getMessage().contains(
                        "Inspection product does not belong to goods receipt"
                )
        );

        verifyNoInteractions(
                discrepancyReportRepository,
                discrepancyItemRepository
        );
    }

    // Test 3: Tu choi phien kiem dem khong co du lieu.
    @Test
    void shouldRejectEmptyInspectionSession() {

        GoodsReceipts receipt = mock(GoodsReceipts.class);
        WarehouseTaskClaim taskClaim = mock(WarehouseTaskClaim.class);
        Users staff = mock(Users.class);

        when(receipt.getId()).thenReturn(43L);
        when(taskClaim.getId()).thenReturn(3L);

        when(receiptInspectionRepository.findByTaskClaimId(3L))
                .thenReturn(List.of());

        BadRequest exception = assertThrows(
                BadRequest.class,
                () -> service.syncFromFinishedInspection(
                        receipt,
                        taskClaim,
                        staff
                )
        );

        assertTrue(
                exception.getMessage().contains(
                        "No receipt inspections found for task claim"
                )
        );

        verifyNoInteractions(
                discrepancyReportRepository,
                discrepancyItemRepository
        );
    }
}
