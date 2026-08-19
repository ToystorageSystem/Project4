package com.toystorage.backend.controllers.stores;

import com.toystorage.backend.dto.request.stores.InspectReturnedItemRequest;
import com.toystorage.backend.dto.request.stores.ReturnedPackageScanRequest;

import com.toystorage.backend.dto.response.stores.ReturnedGoodsDetailResponse;
import com.toystorage.backend.dto.response.stores.ReturnedGoodsListResponse;
import com.toystorage.backend.dto.response.stores.ReturnedPackageResponse;

import com.toystorage.backend.services.stores.ReturnedGoodsEvidenceService;
import com.toystorage.backend.services.stores.ReturnedGoodsInspectionService;
import com.toystorage.backend.services.stores.ReturnedGoodsQueryService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(
        "/api/warehouse/staff/store-returns"
)
@RequiredArgsConstructor
public class ReturnedGoodsInspectionController {

    private final ReturnedGoodsQueryService
            queryService;

    private final ReturnedGoodsInspectionService
            inspectionService;

    private final ReturnedGoodsEvidenceService
            evidenceService;


    // =====================================================
    // WAITING LIST
    // =====================================================

    @GetMapping
//    @PreAuthorize(
//            "hasAuthority('STORE_RETURN_VIEW')"
//    )
    public ResponseEntity<List<ReturnedGoodsListResponse>>
    getWaitingReturns() {

        return ResponseEntity.ok(
                queryService.getWaitingReturns()
        );
    }


    // =====================================================
    // DETAIL
    // =====================================================

    @GetMapping("/{returnId}")
//    @PreAuthorize(
//            "hasAuthority('STORE_RETURN_VIEW')"
//    )
    public ResponseEntity<ReturnedGoodsDetailResponse>
    getDetail(
            @PathVariable Long returnId
    ) {

        return ResponseEntity.ok(
                queryService.getDetail(
                        returnId
                )
        );
    }


    // =====================================================
    // START INSPECTION
    // =====================================================

    @PatchMapping(
            "/{returnId}/start"
    )
//    @PreAuthorize(
//            "hasAuthority('STORE_RETURN_RECEIVE')"
//    )
    public ResponseEntity<ReturnedGoodsDetailResponse>
    startInspection(
            @PathVariable Long returnId
    ) {

        return ResponseEntity.ok(
                inspectionService.startInspection(
                        returnId
                )
        );
    }


    // =====================================================
    // SCAN PACKAGE
    // =====================================================

    @PostMapping(
            "/{returnId}/scan-package"
    )
    @PreAuthorize(
            "hasAuthority('STORE_RETURN_RECEIVE')"
    )
    public ResponseEntity<ReturnedPackageResponse>
    scanPackage(
            @PathVariable Long returnId,

            @Valid
            @RequestBody
            ReturnedPackageScanRequest request
    ) {

        return ResponseEntity.ok(
                inspectionService.scanPackage(
                        returnId,
                        request
                )
        );
    }


    // =====================================================
    // INSPECT PRODUCT
    // =====================================================

    @PutMapping(
            "/{returnId}/items"
    )
    @PreAuthorize(
            "hasAuthority('STORE_RETURN_RECEIVE')"
    )
    public ResponseEntity<ReturnedGoodsDetailResponse>
    inspectItem(
            @PathVariable Long returnId,

            @Valid
            @RequestBody
            InspectReturnedItemRequest request
    ) {

        return ResponseEntity.ok(
                inspectionService.inspectItem(
                        returnId,
                        request
                )
        );
    }


    // =====================================================
    // UPLOAD EVIDENCE
    // =====================================================

    @PostMapping(
            value = "/{returnId}/items/{itemId}/evidence",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize(
            "hasAuthority('STORE_RETURN_RECEIVE')"
    )
    public ResponseEntity<Map<String, String>>
    uploadEvidence(
            @PathVariable Long returnId,

            @PathVariable Long itemId,

            @RequestPart("image")
            MultipartFile image
    ) {

        String imageUrl =
                evidenceService.uploadEvidence(
                        returnId,
                        itemId,
                        image
                );


        return ResponseEntity.ok(
                Map.of(
                        "imageUrl",
                        imageUrl
                )
        );
    }


    // =====================================================
    // SUBMIT TO MANAGER
    // =====================================================

    @PatchMapping(
            "/{returnId}/submit"
    )
    @PreAuthorize(
            "hasAuthority('STORE_RETURN_RECEIVE')"
    )
    public ResponseEntity<ReturnedGoodsDetailResponse>
    submitInspection(
            @PathVariable Long returnId
    ) {

        return ResponseEntity.ok(
                inspectionService.submitInspection(
                        returnId
                )
        );
    }
}