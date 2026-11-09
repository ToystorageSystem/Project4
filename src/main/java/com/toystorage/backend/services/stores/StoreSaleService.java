package com.toystorage.backend.services.stores;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toystorage.backend.dto.request.stores.CreateSaleRequest;
import com.toystorage.backend.dto.request.stores.SaleLineRequest;
import com.toystorage.backend.dto.response.common.PageResponse;
import com.toystorage.backend.dto.response.stores.SaleDetail;
import com.toystorage.backend.dto.response.stores.SaleSummary;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.stores.StoreSaleItems;
import com.toystorage.backend.entity.stores.StoreSales;
import com.toystorage.backend.entity.stores.StoreStockTransactions;
import com.toystorage.backend.entity.stores.StoreStocks;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.products.ProductStatus;
import com.toystorage.backend.enums.stores.StockTransactionType;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.mapper.stores.StoreSaleMapper;
import com.toystorage.backend.repository.products.ProductRepository;
import com.toystorage.backend.repository.receipts.store.StoreStockRepository;
import com.toystorage.backend.repository.stores.StoreSaleRepository;
import com.toystorage.backend.repository.receipts.store.StoreStockTransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StoreSaleService {

    public static final int MAX_PAGE_SIZE = 50;

    private final StoreAccessService storeAccessService;
    private final ProductRepository productRepository;
    private final StoreStockRepository stockRepository;
    private final StoreSaleRepository saleRepository;
    private final StoreStockTransactionRepository transactionRepository;
    private final StoreSaleMapper mapper;

    /**
     * POST /api/store/sales
     * All lines in ONE transaction: any error -> whole sale rolled back, stock unchanged.
     */
    @Transactional
    public SaleDetail createSale(Long userId, CreateSaleRequest request) {
        Users seller = storeAccessService.requireStoreUser(userId);
        Long storeId = seller.getWarehouse().getId();

        // 1. no duplicate product (DB: UNIQUE sale_id + product_id)
        Set<Long> productIds = new LinkedHashSet<>();
        for (SaleLineRequest line : request.items()) {
            if (!productIds.add(line.productId())) {
                throw new BadRequest("Duplicate product in sale");
            }
        }

        // 2. load all products + stock rows: 2 queries for the whole sale
        Map<Long, Products> products = productRepository
                .findByIdInAndStatus(productIds, ProductStatus.ACTIVE).stream()
                .collect(Collectors.toMap(Products::getId, Function.identity()));
        Map<Long, StoreStocks> stocks = stockRepository
                .findByStoreIdAndProductIdIn(storeId, new ArrayList<>(productIds)).stream()
                .collect(Collectors.toMap(s -> s.getProduct().getId(), Function.identity()));

        // 3. take stock line by line (shelf first, then backroom)
        StoreSales sale = StoreSales.create(newSaleCode(storeId), storeId, normalize(request.note()), seller);
        for (SaleLineRequest line : request.items()) {
            Products product = products.get(line.productId());
            if (product == null) {
                throw new NotFound("Product not found: " + line.productId());
            }
            StoreStocks stock = stocks.get(line.productId());
            int available = stock == null ? 0 : stock.getTotalQuantity();
            if (available < line.quantity()) {
                throw new BadRequest("Not enough stock: " + product.getProductsCode()
                        + " (available " + available + ")");
            }
            int fromShelf = stock.sell(line.quantity());
            sale.addItem(product, line.quantity(), product.getSellingPrice(), fromShelf);
        }

        // 4. save sale + items (cascade) -> sale id for the history rows
        saleRepository.save(sale);

        // 5. stock history: 1 SALE row per product
        LocalDateTime now = LocalDateTime.now();
        List<StoreStockTransactions> history = new ArrayList<>();
        for (StoreSaleItems item : sale.getItems()) {
            StoreStocks stock = stocks.get(item.getProduct().getId());
            history.add(StoreStockTransactions.builder()
                    .code("STX-S" + sale.getId() + "-" + item.getProduct().getId())
                    .storeId(storeId)
                    .product(item.getProduct())
                    .type(StockTransactionType.SALE)
                    .shelfChange(-item.getFromShelf())
                    .backroomChange(-item.getFromBackroom())
                    .shelfAfter(stock.getShelfQuantity())
                    .backroomAfter(stock.getBackroomQuantity())
                    .saleId(sale.getId())
                    .note(sale.getCode())
                    .createdBy(seller)
                    .createdAt(now)
                    .build());
        }
        transactionRepository.saveAll(history);

        return mapper.toDetail(sale);   // stock rows are managed -> updated at commit (@Version checked)
    }

    /** GET /api/store/sales?page=&size= */
    @Transactional(readOnly = true)
    public PageResponse<SaleSummary> listSales(Long userId, int page, int size) {
        Long storeId = storeAccessService.requireStoreId(userId);
        if (page < 0) {
            throw new BadRequest("Page must be >= 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequest("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return PageResponse.from(saleRepository
                .findByStoreIdOrderByCreatedAtDescIdDesc(storeId, PageRequest.of(page, size))
                .map(mapper::toSummary));
    }

    /** GET /api/store/sales/{id} */
    @Transactional(readOnly = true)
    public SaleDetail getSale(Long userId, Long saleId) {
        Long storeId = storeAccessService.requireStoreId(userId);
        return saleRepository.findByIdAndStoreId(saleId, storeId)
                .map(mapper::toDetail)
                .orElseThrow(() -> new NotFound("Sale not found"));
    }

    // ---------- helpers ----------

    /** e.g. SL-14-3F9A0C1B2D4E */
    private static String newSaleCode(Long storeId) {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return "SL-" + storeId + "-" + random;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}