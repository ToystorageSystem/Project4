package com.toystorage.backend.controllers.inventories.shortage;

import com.toystorage.backend.dto.request.inventories.shortage.CreateReceivingShortageRequest;
import com.toystorage.backend.dto.response.inventories.shortage.ReceivingShortageReportResponse;
import com.toystorage.backend.services.inventories.shortage.ReceivingShortageReportService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse/receiving-shortages")
@RequiredArgsConstructor
public class ReceivingShortageReportController {

    private final ReceivingShortageReportService
            receivingShortageReportService;


    @PostMapping(
            value = "/goods-receipts/{receiptId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
//    @PreAuthorize(
//            "hasAuthority('RECEIVING_SHORTAGE_CREATE')"
//    )
    public ResponseEntity<ReceivingShortageReportResponse> createReport(
            @PathVariable("receiptId") Long receiptId,

            @RequestParam("productId") Long productId,

            @RequestParam("description") String description,

            @RequestPart(
                    value = "image",
                    required = false
            )
            MultipartFile image
    ) {

        CreateReceivingShortageRequest request =
                new CreateReceivingShortageRequest();

        request.setProductId(productId);
        request.setDescription(description);

        return ResponseEntity.ok(
                receivingShortageReportService
                        .createReport(
                                receiptId,
                                request,
                                image
                        )
        );
    }

    @GetMapping("/my-reports")
//    @PreAuthorize(
//            "hasAuthority('RECEIVING_SHORTAGE_VIEW')"
//    )
    public ResponseEntity<List<ReceivingShortageReportResponse>>
    getMyReports() {

        return ResponseEntity.ok(
                receivingShortageReportService
                        .getMyReports()
        );
    }
}