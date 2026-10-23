package com.toystorage.backend.services.suppliers;

import com.toystorage.backend.dto.request.suppliers.CreateSupplierRequest;
import com.toystorage.backend.dto.request.suppliers.UpdateSupplierRequest;
import com.toystorage.backend.dto.response.suppliers.SupplierPageResponse;
import com.toystorage.backend.dto.response.suppliers.SupplierResponse;
import com.toystorage.backend.entity.suppliers.Suppliers;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.products.CommonStatus;
import com.toystorage.backend.enums.receipts.PurchaseOrderStatus;
import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.Conflict;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.mapper.suppliers.SupplierMapper;
import com.toystorage.backend.repository.receipts.PurchaseOrderRepository;
import com.toystorage.backend.repository.suppliers.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private static final Set<String>
            ALLOWED_SORT_FIELDS = Set.of(
                    "suppliersCode",
                    "name",
                    "phone",
                    "email",
                    "status",
                    "createdAt",
                    "updatedAt"
            );

    private static final List<PurchaseOrderStatus>
            COMPLETED_ORDER_STATUSES = List.of(
                    PurchaseOrderStatus.COMPLETED,
                    PurchaseOrderStatus.CANCELLED
            );

    private final SupplierRepository
            supplierRepository;

    private final PurchaseOrderRepository
            purchaseOrderRepository;

    private final SupplierMapper
            supplierMapper;

    private final SupplierAuditService
            supplierAuditService;

    /*
     * DANH SÁCH, TÌM KIẾM, LỌC VÀ PHÂN TRANG
     */
    @Transactional(readOnly = true)
    public SupplierPageResponse getSuppliers(
            String keyword,
            CommonStatus status,
            int page,
            int size,
            String sortBy,
            Sort.Direction direction
    ) {

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {

            throw new BadRequest(
                    "Unsupported supplier sort field: "
                            + sortBy
            );
        }

        PageRequest pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                direction,
                                sortBy
                        )
                );

        Page<Suppliers> result =
                supplierRepository.search(
                        trimToNull(keyword),
                        status,
                        pageable
                );

        List<SupplierResponse> content =
                result.getContent()
                        .stream()
                        .map(
                                supplierMapper::toResponse
                        )
                        .toList();

        return SupplierPageResponse.builder()

                .content(content)

                .page(
                        result.getNumber()
                )

                .size(
                        result.getSize()
                )

                .totalElements(
                        result.getTotalElements()
                )

                .totalPages(
                        result.getTotalPages()
                )

                .first(
                        result.isFirst()
                )

                .last(
                        result.isLast()
                )

                .build();
    }

    /*
     * XEM CHI TIẾT
     */
    @Transactional(readOnly = true)
    public SupplierResponse getSupplier(
            Long supplierId
    ) {

        Suppliers supplier =
                findSupplier(supplierId);

        return supplierMapper.toResponse(
                supplier
        );
    }

    /*
     * TẠO NHÀ CUNG CẤP
     */
    @Transactional
    public SupplierResponse createSupplier(
            CreateSupplierRequest request,
            Users actor,
            String ipAddress,
            String deviceInfo
    ) {

        normalizeCreateRequest(request);

        validateUniqueData(
                request.getEmail(),
                request.getTaxCode(),
                null
        );

        Suppliers supplier =
                supplierMapper.toEntity(request);

        supplier.setStatus(
                CommonStatus.ACTIVE
        );

        /*
         * Gán mã tạm để database sinh ID.
         */
        supplier.setSuppliersCode(
                "TMP-" + UUID.randomUUID()
        );

        supplier =
                supplierRepository.saveAndFlush(
                        supplier
                );

        /*
         * Sinh mã chính thức theo ID:
         * SUP-000001
         */
        supplier.setSuppliersCode(
                String.format(
                        Locale.ROOT,
                        "SUP-%06d",
                        supplier.getId()
                )
        );

        supplier =
                supplierRepository.saveAndFlush(
                        supplier
                );

        SupplierResponse response =
                supplierMapper.toResponse(
                        supplier
                );

        supplierAuditService.record(
                actor,
                ActivityAction.CREATE,
                supplier.getId(),
                null,
                response,
                ipAddress,
                deviceInfo
        );

        return response;
    }

    /*
     * CẬP NHẬT NHÀ CUNG CẤP
     */
    @Transactional
    public SupplierResponse updateSupplier(
            Long supplierId,
            UpdateSupplierRequest request,
            Users actor,
            String ipAddress,
            String deviceInfo
    ) {

        Suppliers supplier =
                findSupplier(supplierId);

        SupplierResponse oldValue =
                supplierMapper.toResponse(
                        supplier
                );

        normalizeUpdateRequest(request);

        validateUniqueData(
                request.getEmail(),
                request.getTaxCode(),
                supplierId
        );

        supplierMapper.updateEntity(
                request,
                supplier
        );

        supplier =
                supplierRepository.saveAndFlush(
                        supplier
                );

        SupplierResponse response =
                supplierMapper.toResponse(
                        supplier
                );

        supplierAuditService.record(
                actor,
                ActivityAction.UPDATE,
                supplierId,
                oldValue,
                response,
                ipAddress,
                deviceInfo
        );

        return response;
    }

    /*
     * ẨN NHÀ CUNG CẤP
     */
    @Transactional
    public SupplierResponse deactivateSupplier(
            Long supplierId,
            Users actor,
            String ipAddress,
            String deviceInfo
    ) {

        Suppliers supplier =
                findSupplier(supplierId);

        /*
         * Nếu đã bị ẩn thì trả luôn kết quả hiện tại.
         */
        if (supplier.getStatus()
                == CommonStatus.INACTIVE) {

            return supplierMapper.toResponse(
                    supplier
            );
        }

        /*
         * Không được ẩn nếu còn Purchase Order
         * chưa COMPLETED hoặc CANCELLED.
         */
        boolean hasUnfinishedOrder =
                purchaseOrderRepository
                        .existsBySupplier_IdAndStatusNotIn(
                                supplierId,
                                COMPLETED_ORDER_STATUSES
                        );

        if (hasUnfinishedOrder) {

            throw new Conflict(
                    "Không thể ẩn nhà cung cấp "
                            + "vì đang có đơn mua hàng "
                            + "chưa hoàn tất"
            );
        }

        return changeStatus(
                supplier,
                CommonStatus.INACTIVE,
                actor,
                ipAddress,
                deviceInfo
        );
    }

    /*
     * KHÔI PHỤC NHÀ CUNG CẤP
     */
    @Transactional
    public SupplierResponse activateSupplier(
            Long supplierId,
            Users actor,
            String ipAddress,
            String deviceInfo
    ) {

        Suppliers supplier =
                findSupplier(supplierId);

        /*
         * Nếu đã hoạt động thì trả luôn kết quả.
         */
        if (supplier.getStatus()
                == CommonStatus.ACTIVE) {

            return supplierMapper.toResponse(
                    supplier
            );
        }

        return changeStatus(
                supplier,
                CommonStatus.ACTIVE,
                actor,
                ipAddress,
                deviceInfo
        );
    }

    /*
     * THAY ĐỔI TRẠNG THÁI VÀ GHI AUDIT
     */
    private SupplierResponse changeStatus(
            Suppliers supplier,
            CommonStatus newStatus,
            Users actor,
            String ipAddress,
            String deviceInfo
    ) {

        SupplierResponse oldValue =
                supplierMapper.toResponse(
                        supplier
                );

        supplier.setStatus(
                newStatus
        );

        supplier =
                supplierRepository.saveAndFlush(
                        supplier
                );

        SupplierResponse response =
                supplierMapper.toResponse(
                        supplier
                );

        supplierAuditService.record(
                actor,
                ActivityAction.UPDATE,
                supplier.getId(),
                oldValue,
                response,
                ipAddress,
                deviceInfo
        );

        return response;
    }

    /*
     * TÌM SUPPLIER HOẶC NÉM 404
     */
    private Suppliers findSupplier(
            Long supplierId
    ) {

        return supplierRepository
                .findById(supplierId)

                .orElseThrow(() ->
                        new NotFound(
                                "Không tìm thấy nhà cung cấp "
                                        + "có ID: "
                                        + supplierId
                        )
                );
    }

    /*
     * KIỂM TRA EMAIL VÀ MÃ SỐ THUẾ TRÙNG
     */
    private void validateUniqueData(
            String email,
            String taxCode,
            Long currentSupplierId
    ) {

        if (email != null) {

            boolean duplicatedEmail;

            if (currentSupplierId == null) {

                duplicatedEmail =
                        supplierRepository
                                .existsByEmailIgnoreCase(
                                        email
                                );

            } else {

                duplicatedEmail =
                        supplierRepository
                                .existsByEmailIgnoreCaseAndIdNot(
                                        email,
                                        currentSupplierId
                                );
            }

            if (duplicatedEmail) {

                throw new Conflict(
                        "Email nhà cung cấp đã tồn tại"
                );
            }
        }

        if (taxCode != null) {

            boolean duplicatedTaxCode;

            if (currentSupplierId == null) {

                duplicatedTaxCode =
                        supplierRepository
                                .existsByTaxCodeIgnoreCase(
                                        taxCode
                                );

            } else {

                duplicatedTaxCode =
                        supplierRepository
                                .existsByTaxCodeIgnoreCaseAndIdNot(
                                        taxCode,
                                        currentSupplierId
                                );
            }

            if (duplicatedTaxCode) {

                throw new Conflict(
                        "Mã số thuế nhà cung cấp đã tồn tại"
                );
            }
        }
    }

    /*
     * CHUẨN HÓA REQUEST TẠO MỚI
     */
    private void normalizeCreateRequest(
            CreateSupplierRequest request
    ) {

        request.setName(
                request.getName().trim()
        );

        request.setTaxCode(
                upperCaseOrNull(
                        request.getTaxCode()
                )
        );

        request.setContactPerson(
                trimToNull(
                        request.getContactPerson()
                )
        );

        request.setContactPosition(
                trimToNull(
                        request.getContactPosition()
                )
        );

        request.setPhone(
                trimToNull(
                        request.getPhone()
                )
        );

        request.setEmail(
                lowerCaseOrNull(
                        request.getEmail()
                )
        );

        request.setAddress(
                trimToNull(
                        request.getAddress()
                )
        );

        request.setNote(
                trimToNull(
                        request.getNote()
                )
        );
    }

    /*
     * CHUẨN HÓA REQUEST CẬP NHẬT
     */
    private void normalizeUpdateRequest(
            UpdateSupplierRequest request
    ) {

        request.setName(
                request.getName().trim()
        );

        request.setTaxCode(
                upperCaseOrNull(
                        request.getTaxCode()
                )
        );

        request.setContactPerson(
                trimToNull(
                        request.getContactPerson()
                )
        );

        request.setContactPosition(
                trimToNull(
                        request.getContactPosition()
                )
        );

        request.setPhone(
                trimToNull(
                        request.getPhone()
                )
        );

        request.setEmail(
                lowerCaseOrNull(
                        request.getEmail()
                )
        );

        request.setAddress(
                trimToNull(
                        request.getAddress()
                )
        );

        request.setNote(
                trimToNull(
                        request.getNote()
                )
        );
    }

    private String lowerCaseOrNull(
            String value
    ) {

        String normalized =
                trimToNull(value);

        if (normalized == null) {
            return null;
        }

        return normalized.toLowerCase(
                Locale.ROOT
        );
    }

    private String upperCaseOrNull(
            String value
    ) {

        String normalized =
                trimToNull(value);

        if (normalized == null) {
            return null;
        }

        return normalized.toUpperCase(
                Locale.ROOT
        );
    }

    private String trimToNull(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return null;
        }

        return value.trim();
    }
}