package com.toystorage.backend.services.stores;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toystorage.backend.dto.response.common.PageResponse;
import com.toystorage.backend.dto.response.stores.StoreProductDetail;
import com.toystorage.backend.dto.response.stores.StoreProductSummary;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.stores.StoreStocks;
import com.toystorage.backend.enums.products.ProductStatus;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.mapper.stores.StoreProductMapper;
import com.toystorage.backend.repository.products.ProductRepository;
import com.toystorage.backend.repository.receipts.store.StoreStockRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StoreProductService {

    public static final int MAX_PAGE_SIZE = 50;

    private final StoreAccessService storeAccessService;
    private final ProductRepository productRepository;
    private final StoreStockRepository stockRepository;
    private final StoreProductMapper mapper;

    /** GET /api/store/products?keyword=&categoryId=&page=&size= */
    @Transactional(readOnly = true)
    public PageResponse<StoreProductSummary> search(Long userId, String keyword,
                                                    Long categoryId, int page, int size) {
        Long storeId = storeAccessService.requireStoreId(userId);

        if (page < 0) {
            throw new BadRequest("Page must be >= 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequest("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }

        // query 1: one page of ACTIVE products (+ category, brand)
        Page<Products> products = productRepository.searchActive(
                ProductStatus.ACTIVE, normalize(keyword), categoryId,
                PageRequest.of(page, size));

        // query 2: stock rows of exactly those products in this store (+ shelf)
        Map<Long, StoreStocks> stockByProductId = loadStocks(storeId, products.getContent());

        // merge: no stock row -> null -> shelf null, quantities 0
        return PageResponse.from(products.map(
                p -> mapper.toSummary(p, stockByProductId.get(p.getId()))));
    }

    /** GET /api/store/products/{id} */
    @Transactional(readOnly = true)
    public StoreProductDetail getById(Long userId, Long productId) {
        Long storeId = storeAccessService.requireStoreId(userId);
        Products product = productRepository.findByIdAndStatus(productId, ProductStatus.ACTIVE)
                .orElseThrow(StoreProductService::productNotFound);
        return toDetail(storeId, product);
    }

    /** GET /api/store/products/barcode/{barcode} */
    @Transactional(readOnly = true)
    public StoreProductDetail getByBarcode(Long userId, String barcode) {
        Long storeId = storeAccessService.requireStoreId(userId);
        String code = normalize(barcode);
        if (code == null) {
            throw productNotFound();
        }
        List<Products> matches = productRepository.findAllByBarcodeAndStatus(code, ProductStatus.ACTIVE);
        if (matches.isEmpty()) throw productNotFound();
        if (matches.size() > 1) {
            // Quan ly kho/bán hàng can chot logic ton kho dung chung Barcode truoc khi scan.
            throw new BadRequest("Barcode matches multiple supplier products. Please use the product list");
        }
        return toDetail(storeId, matches.get(0));
    }

    // ---------- helpers ----------

    private StoreProductDetail toDetail(Long storeId, Products product) {
        StoreStocks stock = stockRepository
                .findByStoreIdAndProductId(storeId, product.getId())
                .orElse(null);
        return mapper.toDetail(product, stock);
    }

    private Map<Long, StoreStocks> loadStocks(Long storeId, List<Products> products) {
        if (products.isEmpty()) {
            return Map.of();                        // empty page -> skip 2nd query
        }
        List<Long> ids = products.stream().map(Products::getId).toList();
        return stockRepository.findByStoreIdAndProductIdIn(storeId, ids).stream()
                .collect(Collectors.toMap(s -> s.getProduct().getId(), Function.identity()));
    }

    /** trim; blank -> null (= "no filter") */
    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static NotFound productNotFound() {
        return new NotFound("Product not found");
    }
}