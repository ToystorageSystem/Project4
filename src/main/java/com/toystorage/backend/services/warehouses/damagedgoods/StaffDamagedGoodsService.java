package com.toystorage.backend.services.warehouses.damagedgoods;

import com.toystorage.backend.dto.request.warehouses.damagedgoods.CreateStaffDamagedGoodsRequest;

import com.toystorage.backend.dto.response.warehouses.damagedgoods.StaffDamagedGoodsReportResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffDamagedGoodsService {

    private final StaffDamagedGoodsCreationService
            creationService;

    private final StaffDamagedGoodsQueryService
            queryService;


    public StaffDamagedGoodsReportResponse create(
            CreateStaffDamagedGoodsRequest request,
            MultipartFile image
    ) {

        return creationService.create(
                request,
                image
        );
    }


    public List<StaffDamagedGoodsReportResponse>
    getMyReports() {

        return queryService
                .getMyReports();
    }


    public StaffDamagedGoodsReportResponse getDetail(
            Long reportId
    ) {

        return queryService
                .getDetail(
                        reportId
                );
    }
}