package com.toystorage.backend.services.products;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toystorage.backend.dto.request.products.CreateProductRequest;
import com.toystorage.backend.dto.request.products.UpdateProductRequest;
import com.toystorage.backend.dto.response.products.ProductResponse;
import com.toystorage.backend.entity.products.Categories;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.products.ProductStatus;
import com.toystorage.backend.mapper.products.ProductMapper;
import com.toystorage.backend.repository.products.ProductChangeRequestRepository;
import com.toystorage.backend.repository.products.ProductRepository;
import com.toystorage.backend.repository.users.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Kiểm thử logic mới bằng mock; KHÔNG kết nối và ghi vào database nhóm. */
@ExtendWith(MockitoExtension.class)
class ProductServiceBusinessOperationsTest {
    @Mock ProductRepository productRepository;
    @Mock ProductChangeRequestRepository changeRequestRepository;
    @Mock UserRepository userRepository;
    @Mock ProductValidationService validationService;
    @Mock ProductMapper productMapper;
    @Mock ProductAuditService productAuditService;
    @Mock ObjectMapper objectMapper;
    @InjectMocks ProductService service;

    private final Users actor = Users.builder().id(5L).name("Người kiểm thử").build();
    private final Categories category = Categories.builder().id(2L).name("Đồ chơi").build();

    @BeforeEach
    void login() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("biz@example.com", "N/A", java.util.List.of()));
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createProduct_directlyActivatesAndAudits_withoutApprovalRequest() {
        CreateProductRequest input = new CreateProductRequest();
        input.setCategoryId(2L);
        input.setRequestReason("Thêm mẫu hàng mới");
        Products created = Products.builder().id(11L).status(ProductStatus.PENDING_CREATE).build();
        ProductResponse output = ProductResponse.builder().id(11L).status(ProductStatus.ACTIVE).build();

        when(validationService.getActiveCategory(2L)).thenReturn(category);
        when(userRepository.findByEmail("biz@example.com")).thenReturn(Optional.of(actor));
        when(productMapper.toEntity(eq(input), eq(category), isNull(), eq(actor))).thenReturn(created);
        when(productRepository.saveAndFlush(created)).thenReturn(created);
        when(productMapper.toResponse(created)).thenReturn(output);

        ProductResponse actual = service.createProduct(input);

        assertEquals(ProductStatus.ACTIVE, created.getStatus());
        assertEquals(11L, actual.getId());
        verify(productAuditService).logCreate(created, actor, "Thêm mẫu hàng mới");
        verify(changeRequestRepository, never()).save(any());
    }

    @Test
    void updateProduct_updatesExistingDataAndAudits_withoutApprovalRequest() {
        UpdateProductRequest input = new UpdateProductRequest();
        input.setCategoryId(2L);
        input.setRequestReason("Đổi giá niêm yết");
        Products product = Products.builder()
                .id(11L).productsCode("SP-001").barcode("123")
                .name("Xe đồ chơi").baseUnit("PCS")
                .sellingPrice(new BigDecimal("100000.00"))
                .purchasePrice(new BigDecimal("80000.00"))
                .status(ProductStatus.ACTIVE).category(category).build();
        ProductResponse proposal = ProductResponse.builder()
                .productCode("SP-001").barcode("123")
                .name("Xe đồ chơi").baseUnit("PCS")
                .sellingPrice(new BigDecimal("120000.00"))
                .purchasePrice(new BigDecimal("80000.00"))
                .status(ProductStatus.ACTIVE).categoryId(2L).build();

        when(validationService.getProduct(11L)).thenReturn(product);
        when(validationService.getActiveCategory(2L)).thenReturn(category);
        when(userRepository.findByEmail("biz@example.com")).thenReturn(Optional.of(actor));
        when(productAuditService.snapshot(product)).thenReturn(Map.of("sellingPrice", "100000"));
        when(productMapper.toProposedResponse(product, input, category, null)).thenReturn(proposal);
        when(productRepository.saveAndFlush(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(proposal);

        service.updateProduct(11L, input);

        assertEquals(new BigDecimal("120000.00"), product.getSellingPrice());
        verify(productAuditService).logUpdate(eq(product), eq(actor), anyMap(), eq("Đổi giá niêm yết"));
        verify(changeRequestRepository, never()).save(any());
    }
}
