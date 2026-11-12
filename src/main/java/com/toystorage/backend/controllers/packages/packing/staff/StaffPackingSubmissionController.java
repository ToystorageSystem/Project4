package com.toystorage.backend.controllers.packages.packing.staff;

import com.toystorage.backend.dto.response.packages.packing.StaffPackingSubmissionResponse;

import com.toystorage.backend.services.packages.packing.StaffPackingSubmissionService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
        "/api/warehouse/staff/packing-submissions"
)
@RequiredArgsConstructor
public class StaffPackingSubmissionController {

    private final StaffPackingSubmissionService
            submissionService;


    // =====================================================
    // PREVIEW BEFORE SUBMIT
    // =====================================================

    @GetMapping(
            "/transfers/{transferId}"
    )
//    @PreAuthorize(
//            "hasAuthority('TRANSFER_PACK')"
//    )
    public ResponseEntity<StaffPackingSubmissionResponse>
    preview(
            @PathVariable Long transferId
    ) {

        return ResponseEntity.ok(
                submissionService.preview(
                        transferId
                )
        );
    }


    // =====================================================
    // SUBMIT TO WAREHOUSE MANAGER
    // =====================================================

    @PostMapping(
            "/transfers/{transferId}/submit"
    )
//    @PreAuthorize(
//            "hasAuthority('TRANSFER_PACK')"
//    )
    public ResponseEntity<StaffPackingSubmissionResponse>
    submit(
            @PathVariable Long transferId
    ) {

        return ResponseEntity.ok(
                submissionService.submit(
                        transferId
                )
        );
    }
}