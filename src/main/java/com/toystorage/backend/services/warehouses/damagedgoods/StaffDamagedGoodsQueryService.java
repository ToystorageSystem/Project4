package com.toystorage.backend.services.warehouses.damagedgoods;

import com.toystorage.backend.dto.response.warehouses.damagedgoods.StaffDamagedGoodsReportResponse;

import com.toystorage.backend.entity.warehouses.DamagedGoodsItems;
import com.toystorage.backend.entity.warehouses.DamagedGoodsReports;

import com.toystorage.backend.entity.users.Users;

import com.toystorage.backend.mapper.warehouses.damagedgoods.StaffDamagedGoodsMapper;

import com.toystorage.backend.repository.warehouses.damagedgoods.DamagedGoodsItemRepository;
import com.toystorage.backend.repository.warehouses.damagedgoods.DamagedGoodsReportRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffDamagedGoodsQueryService {

    private final DamagedGoodsReportRepository
            reportRepository;

    private final DamagedGoodsItemRepository
            itemRepository;

    private final StaffDamagedGoodsValidationService
            validationService;

    private final StaffDamagedGoodsMapper
            mapper;


    // =====================================================
    // MY REPORTS
    // =====================================================

    @Transactional(readOnly = true)
    public List<StaffDamagedGoodsReportResponse>
    getMyReports() {

        Users staff =
                validationService
                        .getCurrentUser();


        return reportRepository
                .findByReportedByIdOrderByCreatedAtDesc(
                        staff.getId()
                )

                .stream()

                .map(this::buildResponse)

                .toList();
    }


    // =====================================================
    // DETAIL
    // =====================================================

    @Transactional(readOnly = true)
    public StaffDamagedGoodsReportResponse getDetail(
            Long reportId
    ) {

        Users staff =
                validationService
                        .getCurrentUser();


        DamagedGoodsReports report =
                validationService
                        .getMyReport(
                                reportId,
                                staff
                        );


        return buildResponse(
                report
        );
    }


    private StaffDamagedGoodsReportResponse buildResponse(
            DamagedGoodsReports report
    ) {

        List<DamagedGoodsItems> items =
                itemRepository
                        .findByDamagedGoodsReportId(
                                report.getId()
                        );


        return mapper.toResponse(
                report,
                items
        );
    }
}