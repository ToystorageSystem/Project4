package com.toystorage.backend.controllers.packages.packing.staff;

import com.toystorage.backend.dto.response.packages.packing.StaffPackingSubmissionResponse;
import com.toystorage.backend.services.packages.packing.StaffPackingSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/warehouse/staff/packing-submissions/transfers")
@RequiredArgsConstructor
public class StaffPackingQueueController {
    private final StaffPackingSubmissionService service;

    @GetMapping
     // @PreAuthorize("hasAnyAuthority('TRANSFER_PACK','ROLE_WAREHOUSE_STAFF','ROLE_ROLE-WH-STAFF','ROLE_WH_STAFF','ROLE-WH-STAFF')")
    public List<StaffPackingSubmissionResponse> list() {
        return service.getPackingQueue();
    }
}
