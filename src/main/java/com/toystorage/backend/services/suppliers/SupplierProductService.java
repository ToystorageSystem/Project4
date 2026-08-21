package com.toystorage.backend.services.suppliers;

import com.toystorage.backend.dto.request.suppliers.CreateSupplierProductRequest;
import com.toystorage.backend.dto.request.suppliers.UpdateSupplierProductRequest;
import com.toystorage.backend.dto.response.suppliers.SupplierProductResponse;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.suppliers.SupplierProducts;
import com.toystorage.backend.entity.suppliers.Suppliers;
import com.toystorage.backend.entity.users.ActivityLogs;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.users.ActivityAction;
import com.toystorage.backend.enums.users.ActivityEntityType;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.exceptions.Unauthorized;
import com.toystorage.backend.mapper.suppliers.SupplierProductMapper;
import com.toystorage.backend.repository.products.ProductRepository;
import com.toystorage.backend.repository.suppliers.SupplierProductRepository;
import com.toystorage.backend.repository.suppliers.SupplierRepository;
import com.toystorage.backend.repository.users.ActivityLogRepository;
import com.toystorage.backend.repository.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupplierProductService {

    private final SupplierProductRepository supplierProductRepository;

    private final SupplierRepository supplierRepository;

    private final ProductRepository productRepository;

    private final SupplierProductMapper supplierProductMapper;

    private final ActivityLogRepository activityLogRepository;

    private final UserRepository userRepository;


    // =====================================================
    // GET SUPPLIERS OF PRODUCT
    // =====================================================

    @Transactional(readOnly = true)
    public List<SupplierProductResponse> getSuppliersByProduct(
            Long productId
    ) {

        if (!productRepository.existsById(productId)) {
            throw new NotFound(
                    "Product not found with id: " + productId
            );
        }

        return supplierProductRepository
                .findByProduct_IdOrderByIsDefaultDescSupplier_NameAsc(
                        productId
                )
                .stream()
                .map(supplierProductMapper::toResponse)
                .toList();
    }


    // =====================================================
    // GET PRODUCTS OF SUPPLIER
    // =====================================================

    @Transactional(readOnly = true)
    public List<SupplierProductResponse> getProductsBySupplier(
            Long supplierId
    ) {

        if (!supplierRepository.existsById(supplierId)) {
            throw new NotFound(
                    "Supplier not found with id: " + supplierId
            );
        }

        return supplierProductRepository
                .findBySupplier_IdOrderByProduct_NameAsc(
                        supplierId
                )
                .stream()
                .map(supplierProductMapper::toResponse)
                .toList();
    }


    // =====================================================
    // CREATE LINK
    // =====================================================

    @Transactional
    public SupplierProductResponse create(
            CreateSupplierProductRequest request
    ) {

        // 1. Kiểm tra Product
        Products product =
                productRepository
                        .findById(request.getProductId())
                        .orElseThrow(() ->
                                new NotFound(
                                        "Product not found with id: "
                                                + request.getProductId()
                                )
                        );


        // 2. Kiểm tra Supplier
        Suppliers supplier =
                supplierRepository
                        .findById(request.getSupplierId())
                        .orElseThrow(() ->
                                new NotFound(
                                        "Supplier not found with id: "
                                                + request.getSupplierId()
                                )
                        );


        // 3. Không cho tạo liên kết trùng
        if (
                supplierProductRepository
                        .existsBySupplier_IdAndProduct_Id(
                                supplier.getId(),
                                product.getId()
                        )
        ) {

            throw new BadRequest(
                    "Supplier "
                            + supplier.getId()
                            + " is already linked to product "
                            + product.getId()
            );
        }


        // 4. User đang đăng nhập
        Users currentUser = getCurrentUser();


        // 5. Nếu link mới được chọn làm default
        // thì bỏ default của NCC cũ.
        if (Boolean.TRUE.equals(request.getDefaultSupplier())) {

            supplierProductRepository
                    .findByProduct_IdAndIsDefaultTrue(
                            product.getId()
                    )
                    .ifPresent(currentDefault -> {

                        String oldValue =
                                snapshot(currentDefault);

                        currentDefault.setIsDefault(false);

                        supplierProductRepository.save(
                                currentDefault
                        );

                        saveHistory(
                                currentUser,
                                ActivityAction.UPDATE,
                                currentDefault,
                                oldValue,
                                snapshot(currentDefault)
                        );
                    });
        }


        // 6. Tạo liên kết mới
        SupplierProducts entity =
                SupplierProducts.builder()
                        .supplierProductsCode(
                                generateLinkCode()
                        )
                        .supplier(supplier)
                        .product(product)
                        .supplierProductCode(
                                request.getSupplierProductCode()
                        )
                        .purchasePrice(
                                request.getPurchasePrice()
                        )
                        .leadTimeDays(
                                request.getLeadTimeDays()
                        )
                        .minimumOrderQuantity(
                                request.getMinimumOrderQuantity()
                        )
                        .isDefault(
                                Boolean.TRUE.equals(
                                        request.getDefaultSupplier()
                                )
                        )
                        .build();


        entity =
                supplierProductRepository.save(entity);


        // 7. Lưu lịch sử CREATE
        saveHistory(
                currentUser,
                ActivityAction.CREATE,
                entity,
                null,
                snapshot(entity)
        );


        return supplierProductMapper.toResponse(entity);
    }


    // =====================================================
    // UPDATE LINK
    // =====================================================

    @Transactional
    public SupplierProductResponse update(
            Long linkId,
            UpdateSupplierProductRequest request
    ) {

        SupplierProducts entity =
                getLink(linkId);


        Users currentUser =
                getCurrentUser();


        String oldValue =
                snapshot(entity);


        // Chỉ update thông tin của liên kết.
        // Không đổi Product hoặc Supplier.
        entity.setSupplierProductCode(
                request.getSupplierProductCode()
        );

        entity.setPurchasePrice(
                request.getPurchasePrice()
        );

        entity.setLeadTimeDays(
                request.getLeadTimeDays()
        );

        entity.setMinimumOrderQuantity(
                request.getMinimumOrderQuantity()
        );

        entity.setStatus(
                request.getStatus()
        );


        entity =
                supplierProductRepository.save(entity);


        // Lưu lịch sử UPDATE
        saveHistory(
                currentUser,
                ActivityAction.UPDATE,
                entity,
                oldValue,
                snapshot(entity)
        );


        return supplierProductMapper.toResponse(entity);
    }


    // =====================================================
    // SET DEFAULT SUPPLIER
    // =====================================================

    @Transactional
    public SupplierProductResponse setDefault(
            Long linkId
    ) {

        SupplierProducts target =
                getLink(linkId);


        Users currentUser =
                getCurrentUser();


        // Nếu chính nó đã là default thì không cần sửa gì.
        if (Boolean.TRUE.equals(target.getIsDefault())) {

            return supplierProductMapper.toResponse(
                    target
            );
        }


        // Tìm NCC default hiện tại của cùng sản phẩm.
        supplierProductRepository
                .findByProduct_IdAndIsDefaultTrue(
                        target.getProduct().getId()
                )
                .ifPresent(currentDefault -> {

                    if (
                            !currentDefault
                                    .getId()
                                    .equals(target.getId())
                    ) {

                        String oldValue =
                                snapshot(currentDefault);

                        currentDefault.setIsDefault(false);

                        supplierProductRepository.save(
                                currentDefault
                        );

                        saveHistory(
                                currentUser,
                                ActivityAction.UPDATE,
                                currentDefault,
                                oldValue,
                                snapshot(currentDefault)
                        );
                    }
                });


        String oldTargetValue =
                snapshot(target);


        target.setIsDefault(true);


        // Không gán lại target để tránh lỗi lambda
        supplierProductRepository.save(
                target
        );


        saveHistory(
                currentUser,
                ActivityAction.UPDATE,
                target,
                oldTargetValue,
                snapshot(target)
        );


        return supplierProductMapper.toResponse(
                target
        );
    }


    // =====================================================
    // DELETE LINK
    // =====================================================

    @Transactional
    public void delete(
            Long linkId
    ) {

        SupplierProducts entity =
                getLink(linkId);

        supplierProductRepository.delete(
                entity
        );
    }


    // =====================================================
    // GET LINK
    // =====================================================

    private SupplierProducts getLink(
            Long linkId
    ) {

        return supplierProductRepository
                .findById(linkId)
                .orElseThrow(() ->
                        new NotFound(
                                "Supplier-product link not found with id: "
                                        + linkId
                        )
                );
    }


    // =====================================================
    // CURRENT LOGIN USER
    // =====================================================

    private Users getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        if (
                authentication == null
                        || !authentication.isAuthenticated()
                        || "anonymousUser".equals(
                                authentication.getPrincipal()
                        )
        ) {

            throw new Unauthorized(
                    "User is not authenticated"
            );
        }


        return userRepository
                .findByEmail(
                        authentication.getName()
                )
                .orElseThrow(() ->
                        new NotFound(
                                "Authenticated user not found"
                        )
                );
    }


    // =====================================================
    // SAVE ACTIVITY HISTORY
    // =====================================================

    private void saveHistory(
            Users user,
            ActivityAction action,
            SupplierProducts entity,
            String oldValue,
            String newValue
    ) {

        ActivityLogs activityLog =
                ActivityLogs.builder()
                        .user(user)
                        .action(action)
                        .entityType(
                                ActivityEntityType.SUPPLIER_PRODUCT
                        )
                        .entityId(
                                entity.getId()
                        )
                        .oldValue(oldValue)
                        .newValue(newValue)
                        .build();


        activityLogRepository.save(
                activityLog
        );
    }


    // =====================================================
    // SNAPSHOT FOR AUDIT LOG
    // =====================================================

    private String snapshot(
            SupplierProducts entity
    ) {

        return "productId="
                + entity.getProduct().getId()

                + ", supplierId="
                + entity.getSupplier().getId()

                + ", supplierProductCode="
                + entity.getSupplierProductCode()

                + ", purchasePrice="
                + entity.getPurchasePrice()

                + ", leadTimeDays="
                + entity.getLeadTimeDays()

                + ", minimumOrderQuantity="
                + entity.getMinimumOrderQuantity()

                + ", isDefault="
                + entity.getIsDefault()

                + ", status="
                + entity.getStatus();
    }


    // =====================================================
    // GENERATE LINK CODE
    // =====================================================

    private String generateLinkCode() {

        return "SP-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 16)
                .toUpperCase();
    }
}