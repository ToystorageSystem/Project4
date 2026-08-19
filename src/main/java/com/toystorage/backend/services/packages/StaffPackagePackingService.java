package com.toystorage.backend.services.packages;

import com.toystorage.backend.dto.request.packages.ScanPackageItemRequest;
import com.toystorage.backend.dto.request.packages.SealPackageRequest;

import com.toystorage.backend.dto.response.packages.StaffPackingPackageResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StaffPackagePackingService {

    private final StaffPackageCreationService
            creationService;

    private final StaffPackageItemService
            itemService;

    private final StaffPackageCompletionService
            completionService;


    public StaffPackingPackageResponse createPackage(
            Long transferId
    ) {

        return creationService.createPackage(
                transferId
        );
    }


    public StaffPackingPackageResponse scanItem(
            Long packageId,
            ScanPackageItemRequest request
    ) {

        return itemService.scanItem(
                packageId,
                request
        );
    }


    public StaffPackingPackageResponse getPackage(
            Long packageId
    ) {

        return itemService.getPackage(
                packageId
        );
    }


    public StaffPackingPackageResponse sealPackage(
            Long packageId,
            SealPackageRequest request
    ) {

        return completionService.sealPackage(
                packageId,
                request
        );
    }


    public void completePacking(
            Long transferId
    ) {

        completionService.completePacking(
                transferId
        );
    }
}