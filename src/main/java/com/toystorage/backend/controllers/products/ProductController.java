package com.toystorage.backend.controllers.products;

import com.toystorage.backend.dto.request.products.CreateProductRequest;
import com.toystorage.backend.dto.request.products.CreateProductWithSupplierRequest;
import com.toystorage.backend.dto.request.products.RejectProductRequest;
import com.toystorage.backend.dto.request.products.UpdateProductRequest;
import com.toystorage.backend.dto.response.products.ProductChangeRequestResponse;
import com.toystorage.backend.dto.response.products.ProductActivityResponse;
import com.toystorage.backend.dto.response.products.ProductImageUploadResponse;
import com.toystorage.backend.dto.response.products.ProductPageResponse;
import com.toystorage.backend.dto.response.products.ProductResponse;
import com.toystorage.backend.enums.products.ProductStatus;
import com.toystorage.backend.services.products.ProductService;
import com.toystorage.backend.services.products.ProductImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.toystorage.backend.dto.response.products.ProductImportPreviewResponse;
import com.toystorage.backend.dto.response.products.ProductImportResultResponse;
import com.toystorage.backend.services.products.ProductExcelImportService;
import com.toystorage.backend.services.products.ProductSimpleExcelImportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductExcelImportService productExcelImportService;
    private final ProductSimpleExcelImportService simpleExcelImportService;
    private final ProductImageService productImageService;

    /**
     * Business Operations tạo trực tiếp sản phẩm ACTIVE, có nhật ký thao tác.
     */
    @PostMapping
     // @PreAuthorize("""
             // hasAnyAuthority(
                 // 'PRODUCT_CREATE',
                 // 'ROLE_ROLE-BIZ-OPS',
                 // 'ROLE_ROLE-ADMIN'
             // )
             // """)
    public ResponseEntity<ProductResponse>
    createProduct(
            @Valid
            @RequestBody CreateProductRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        productService.createProduct(request)
                );
    }

    /**
     * Tao Product va lien ket nha cung cap trong cung transaction.
     * Khong cho tao Product khong co nha cung cap tu giao dien moi.
     */
    @PostMapping("/with-supplier")
    // @PreAuthorize("hasAnyAuthority('PRODUCT_CREATE','ROLE_ROLE-BIZ-OPS','ROLE_ROLE-ADMIN')")
    public ResponseEntity<ProductResponse> createProductWithSupplier(
            @Valid @RequestBody CreateProductWithSupplierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProductWithSupplier(request));
    }

    /**
     * API cập nhật trực tiếp. Không tạo phiếu chờ xét duyệt.
     * Giữ đường dẫn API cũ ở dạng 410 để client cũ không tạo dữ liệu chờ mới.
     */
    @PutMapping("/{productId}")
     // @PreAuthorize("hasAnyAuthority('PRODUCT_UPDATE','ROLE_ROLE-BIZ-OPS','ROLE_ROLE-ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(productId, request));
    }

    @PostMapping("/{productId}/change-requests")
     // @PreAuthorize("hasAnyAuthority('PRODUCT_UPDATE','ROLE_ROLE-BIZ-OPS','ROLE_ROLE-ADMIN')")
    public ResponseEntity<Void> deprecatedChangeRequest(@PathVariable Long productId) {
        return ResponseEntity.status(HttpStatus.GONE).build();
    }

    /** Nhật ký thay đổi sản phẩm, chỉ đọc; dữ liệu lấy từ activity_logs. */
    @GetMapping("/{productId}/history")
     // @PreAuthorize("hasAnyAuthority('PRODUCT_VIEW','ROLE_ROLE-BIZ-OPS','ROLE_ROLE-ADMIN')")
    public ResponseEntity<Page<ProductActivityResponse>> getProductHistory(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(productService.getProductHistory(productId, page, size));
    }

    /**
     * Xem danh sách sản phẩm, có thể lọc theo trạng thái.
     */
    @GetMapping
     // @PreAuthorize("""
             // hasAnyAuthority(
                 // 'PRODUCT_VIEW',
                 // 'ROLE_ROLE-BIZ-OPS',
                 // 'ROLE_ROLE-ADMIN'
             // )
             // """)
    public ResponseEntity<ProductPageResponse> getProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                productService.getProducts(
                        status,
                        keyword,
                        categoryId,
                        page,
                        size
                )
        );
    }


    /**
     * Xem chi tiết sản phẩm.
     */
    @GetMapping("/{productId}")
     // @PreAuthorize("""
             // hasAnyAuthority(
                 // 'PRODUCT_VIEW',
                 // 'ROLE_ROLE-BIZ-OPS',
                 // 'ROLE_ROLE-ADMIN'
             // )
             // """)
    public ResponseEntity<ProductResponse>
    getProduct(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(
                productService.getProduct(productId)
        );
    }

    /**
     * Business Staff ẩn sản phẩm đang hoạt động.
     */
    @PatchMapping("/{productId}/deactivate")
     // @PreAuthorize("""
             // hasAnyAuthority(
                 // 'PRODUCT_UPDATE',
                 // 'ROLE_ROLE-BIZ-OPS',
                 // 'ROLE_ROLE-ADMIN'
             // )
             // """)
    public ResponseEntity<ProductResponse>
    deactivateProduct(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(
                productService.deactivateProduct(productId)
        );
    }

    /**
     * Business Staff khôi phục sản phẩm đã ẩn.
     */
    @PatchMapping("/{productId}/activate")
     // @PreAuthorize("""
             // hasAnyAuthority(
                 // 'PRODUCT_UPDATE',
                 // 'ROLE_ROLE-BIZ-OPS',
                 // 'ROLE_ROLE-ADMIN'
             // )
             // """)
    public ResponseEntity<ProductResponse>
    activateProduct(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(
                productService.activateProduct(productId)
        );
    }

    /**
     * Business Manager xem danh sách yêu cầu đang chờ duyệt.
     */
    @GetMapping("/change-requests/pending")
     // @PreAuthorize("""
             // hasAnyAuthority(
                 // 'PRODUCT_APPROVE',
                 // 'ROLE_ROLE-BIZ-OPS',
                 // 'ROLE_ROLE-ADMIN'
             // )
             // """)
    public ResponseEntity<Page<ProductChangeRequestResponse>>
    getPendingRequests(
            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        return ResponseEntity.ok(
                productService.getPendingRequests(
                        page,
                        size
                )
        );
    }

    /**
     * Business Manager xem chi tiết yêu cầu chờ duyệt.
     */
    @GetMapping("/change-requests/{requestId}")
     // @PreAuthorize("""
             // hasAnyAuthority(
                 // 'PRODUCT_APPROVE',
                 // 'ROLE_ROLE-BIZ-OPS',
                 // 'ROLE_ROLE-ADMIN'
             // )
             // """)
    public ResponseEntity<ProductChangeRequestResponse>
    getPendingRequest(
            @PathVariable Long requestId
    ) {
        return ResponseEntity.ok(
                productService.getPendingRequest(
                        requestId
                )
        );
    }

    /**
     * Business Manager phê duyệt yêu cầu.
     */
    @PatchMapping("/change-requests/{requestId}/approve")
     // @PreAuthorize("""
             // hasAnyAuthority(
                 // 'PRODUCT_APPROVE',
                 // 'ROLE_ROLE-BIZ-OPS',
                 // 'ROLE_ROLE-ADMIN'
             // )
             // """)
    public ResponseEntity<ProductChangeRequestResponse>
    approveCreateRequest(
            @PathVariable Long requestId
    ) {
        return ResponseEntity.ok(
                productService.approveCreateRequest(
                        requestId
                )
        );
    }

    /**
     * Business Manager từ chối yêu cầu.
     */
    @PatchMapping("/change-requests/{requestId}/reject")
     // @PreAuthorize("""
             // hasAnyAuthority(
                 // 'PRODUCT_APPROVE',
                 // 'ROLE_ROLE-BIZ-OPS',
                 // 'ROLE_ROLE-ADMIN'
             // )
             // """)
    public ResponseEntity<ProductChangeRequestResponse>
    rejectCreateRequest(
            @PathVariable Long requestId,

            @Valid
            @RequestBody RejectProductRequest request
    ) {
        return ResponseEntity.ok(
                productService.rejectCreateRequest(
                        requestId,
                        request
                )
        );
    }

    /**
     * Business Staff upload ảnh sản phẩm lên Cloudinary.
     *
     * API này chỉ upload ảnh và trả về URL.
     * Chưa tạo hoặc cập nhật Product trong database.
     */
    @PostMapping(
            value = "/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
     // @PreAuthorize("""
             // hasAnyAuthority(
                 // 'PRODUCT_CREATE',
                 // 'PRODUCT_UPDATE',
                 // 'ROLE_ROLE-BIZ-OPS',
                 // 'ROLE_ROLE-ADMIN'
             // )
             // """)
    public ResponseEntity<ProductImageUploadResponse>
    uploadProductImage(
            @RequestParam("image") MultipartFile image
    ) {
        return ResponseEntity.ok(
                productImageService.uploadImage(image)
        );
    }

    // Simple one-sheet import: one Product and one Supplier per row.
    @GetMapping(value = "/import-simple/template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    // @PreAuthorize("hasAnyAuthority('PRODUCT_CREATE','ROLE_ROLE-BIZ-OPS','ROLE_ROLE-ADMIN')")
    public ResponseEntity<byte[]> downloadSimpleImportTemplate() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ToyStorage_Product_Import_Template.xlsx")
                .body(simpleExcelImportService.template());
    }

    @PostMapping(value = "/import-simple/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // @PreAuthorize("hasAnyAuthority('PRODUCT_CREATE','ROLE_ROLE-BIZ-OPS','ROLE_ROLE-ADMIN')")
    public ResponseEntity<ProductImportPreviewResponse> previewSimpleProductImport(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(simpleExcelImportService.preview(file));
    }

    @PostMapping(value = "/import-simple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // @PreAuthorize("hasAnyAuthority('PRODUCT_CREATE','ROLE_ROLE-BIZ-OPS','ROLE_ROLE-ADMIN')")
    public ResponseEntity<ProductImportResultResponse> simpleProductImport(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(simpleExcelImportService.importProducts(file));
    }

    /**
     * Business Staff tải file Excel lên để kiểm tra dữ liệu.
     * API này chỉ preview, chưa lưu sản phẩm vào database.
     */
    @PostMapping(
            value = "/import/preview",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
     // @PreAuthorize("""
             // hasAnyAuthority(
                 // 'PRODUCT_CREATE',
                 // 'ROLE_ROLE-BIZ-OPS',
                 // 'ROLE_ROLE-ADMIN'
             // )
             // """)
    public ResponseEntity<ProductImportPreviewResponse>
    previewProductImport(
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(
                productExcelImportService.preview(file)
        );
    }

    /**
     * Business Staff xác nhận nhập sản phẩm từ file Excel.
     *
     * File sẽ được kiểm tra lại trước khi lưu.
     * Nếu còn dòng không hợp lệ thì toàn bộ file sẽ không được import.
     */
    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
     // @PreAuthorize("""
             // hasAnyAuthority(
                 // 'PRODUCT_CREATE',
                 // 'ROLE_ROLE-BIZ-OPS',
                 // 'ROLE_ROLE-ADMIN'
             // )
             // """)
    public ResponseEntity<ProductImportResultResponse>
    importProducts(
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        productExcelImportService.importProducts(file)
                );
    }
}