package com.toystorage.backend.services.inventories;

import com.toystorage.backend.dto.request.inventories.CreateReceivingShortageRequest;
import com.toystorage.backend.dto.response.inventories.ReceivingShortageReportResponse;

import com.toystorage.backend.entity.inventories.DiscrepancyItems;
import com.toystorage.backend.entity.inventories.DiscrepancyReports;
import com.toystorage.backend.entity.receipts.GoodsReceiptItems;
import com.toystorage.backend.entity.receipts.GoodsReceipts;
import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.enums.inventories.DiscrepancyReferenceType;
import com.toystorage.backend.enums.inventories.DiscrepancyStatus;
import com.toystorage.backend.enums.inventories.DiscrepancyType;
import com.toystorage.backend.services.cloudinary.CloudinaryService;

import com.toystorage.backend.mapper.inventories.ReceivingShortageMapper;

import com.toystorage.backend.repository.inventories.DiscrepancyItemRepository;
import com.toystorage.backend.repository.inventories.DiscrepancyReportRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReceivingShortageReportService {

    private final DiscrepancyReportRepository
            discrepancyReportRepository;

    private final DiscrepancyItemRepository
            discrepancyItemRepository;

    private final ReceivingShortageValidationService
            validationService;

    private final ReceivingShortageMapper
            mapper;

    private final CloudinaryService
            cloudinaryService;


    @Transactional
    public ReceivingShortageReportResponse createReport(
            Long receiptId,
            CreateReceivingShortageRequest request,
            MultipartFile image
    ) {

        Users staff =
                validationService.getCurrentUser();

        GoodsReceipts receipt =
                validationService
                        .getReceipt(receiptId);

        validationService
                .validateSameWarehouse(
                        staff,
                        receipt
                );

        GoodsReceiptItems receiptItem =
                validationService
                        .getReceiptItem(
                                receiptId,
                                request.getProductId()
                        );

        validationService
                .validateShortage(
                        receiptItem
                );


        String imageUrl = null;

        if (image != null
                && !image.isEmpty()) {

            imageUrl =
                    cloudinaryService
                            .uploadImage(image);
        }


        LocalDateTime now =
                LocalDateTime.now();


        DiscrepancyReports report =
                DiscrepancyReports
                        .builder()

                        .discrepancyReportsCode(
                                generateCode("DR")
                        )

                        .reportCode(
                                generateReportCode()
                        )

                        .warehouse(
                                receipt.getWarehouse()
                        )

                        .discrepancyType(
                                DiscrepancyType.SHORTAGE
                        )

                        .referenceType(
                                DiscrepancyReferenceType.GOODS_RECEIPT
                        )

                        .referenceId(
                                receiptId
                        )

                        .description(
                                request.getDescription()
                        )

                        .evidenceImageUrl(
                                imageUrl
                        )

                        .reportedBy(
                                staff
                        )

                        .status(
                                DiscrepancyStatus.OPEN
                        )

                        .createdAt(now)
                        .updatedAt(now)

                        .build();


        report =
                discrepancyReportRepository
                        .save(report);


        int expected =
                receiptItem
                        .getExpectedQuantity();

        int actual =
                receiptItem
                        .getActualQuantity();


        DiscrepancyItems item =
                DiscrepancyItems
                        .builder()

                        .discrepancyItemsCode(
                                generateCode("DI")
                        )

                        .discrepancyReport(
                                report
                        )

                        .product(
                                receiptItem.getProduct()
                        )

                        .expectedQuantity(
                                expected
                        )

                        .actualQuantity(
                                actual
                        )

                        .differenceQuantity(
                                actual - expected
                        )

                        .build();


        discrepancyItemRepository
                .save(item);


        return mapper.toResponse(
                report,
                List.of(item)
        );
    }


    @Transactional(readOnly = true)
    public List<ReceivingShortageReportResponse>
    getMyReports() {

        Users staff =
                validationService
                        .getCurrentUser();

        return discrepancyReportRepository
                .findByReportedByIdOrderByCreatedAtDesc(
                        staff.getId()
                )

                .stream()

                .filter(report ->
                        report.getReferenceType()
                                == DiscrepancyReferenceType.GOODS_RECEIPT
                                &&
                                report.getDiscrepancyType()
                                        == DiscrepancyType.SHORTAGE
                )

                .map(report -> {

                    List<DiscrepancyItems> items =
                            discrepancyItemRepository
                                    .findByDiscrepancyReportId(
                                            report.getId()
                                    );

                    return mapper.toResponse(
                            report,
                            items
                    );
                })

                .toList();
    }


    private String generateReportCode() {

        return "SHORT-"
                + System.currentTimeMillis();
    }


    private String generateCode(
            String prefix
    ) {

        return prefix
                + "-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();
    }
}