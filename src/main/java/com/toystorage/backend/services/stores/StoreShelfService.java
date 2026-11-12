package com.toystorage.backend.services.stores;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toystorage.backend.dto.request.stores.AssignShelfRequest;
import com.toystorage.backend.dto.request.stores.MinShelfRequest;
import com.toystorage.backend.dto.request.stores.MoveStockRequest;
import com.toystorage.backend.dto.response.stores.LowShelfItem;
import com.toystorage.backend.dto.response.stores.ShelfDetail;
import com.toystorage.backend.dto.response.stores.ShelfSummary;
import com.toystorage.backend.dto.response.stores.StoreProductDetail;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.stores.StoreShelves;
import com.toystorage.backend.entity.stores.StoreStockTransactions;
import com.toystorage.backend.entity.stores.StoreStocks;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.enums.products.ProductStatus;
import com.toystorage.backend.enums.stores.MoveDirection;
import com.toystorage.backend.enums.stores.ShelfStatus;
import com.toystorage.backend.enums.stores.StockTransactionType;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.exceptions.NotFound;
import com.toystorage.backend.mapper.stores.StoreProductMapper;
import com.toystorage.backend.mapper.stores.StoreShelfMapper;
import com.toystorage.backend.repository.products.ProductRepository;
import com.toystorage.backend.repository.receipts.store.StoreStockRepository;
import com.toystorage.backend.repository.stores.ShelfUsage;
import com.toystorage.backend.repository.receipts.store.StoreShelfRepository;
import com.toystorage.backend.repository.receipts.store.StoreStockTransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StoreShelfService {

    private final StoreAccessService storeAccessService;
    private final StoreShelfRepository shelfRepository;
    private final StoreStockRepository stockRepository;
    private final StoreStockTransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final StoreShelfMapper shelfMapper;
    private final StoreProductMapper productMapper;

    // ---------- read ----------

    /** GET /api/store/shelves – 2 queries: shelves + usage per shelf */
    @Transactional(readOnly = true)
    public List<ShelfSummary> listShelves(Long userId) {
        Long storeId = storeAccessService.requireStoreId(userId);
        Map<Long, ShelfUsage> usageByShelf = stockRepository.sumByShelf(storeId).stream()
                .collect(Collectors.toMap(ShelfUsage::shelfId, Function.identity()));
        return shelfRepository.findByStoreIdAndStatusOrderByNameAsc(storeId, ShelfStatus.ACTIVE)
                .stream()
                .map(shelf -> shelfMapper.toSummary(shelf, usageByShelf.get(shelf.getId())))
                .toList();
    }

    /** GET /api/store/shelves/{id} */
    @Transactional(readOnly = true)
    public ShelfDetail getShelf(Long userId, Long shelfId) {
        Long storeId = storeAccessService.requireStoreId(userId);
        StoreShelves shelf = requireShelf(storeId, shelfId);
        List<StoreStocks> stocks =
                stockRepository.findByStoreIdAndShelf_IdOrderByProduct_NameAsc(storeId, shelfId);
        return shelfMapper.toDetail(shelf, stocks);
    }

    /** GET /api/store/products/low-shelf */
    @Transactional(readOnly = true)
    public List<LowShelfItem> lowShelf(Long userId) {
        Long storeId = storeAccessService.requireStoreId(userId);
        return stockRepository.findLowShelf(storeId, ProductStatus.ACTIVE).stream()
                .map(shelfMapper::toLowShelf)
                .toList();
    }

    // ---------- write ----------

    /** PUT /api/store/products/{productId}/shelf  { shelfId | null } */
    @Transactional
    public StoreProductDetail assignShelf(Long userId, Long productId, AssignShelfRequest request) {
        Long storeId = storeAccessService.requireStoreId(userId);
        Products product = requireActiveProduct(productId);

        StoreShelves shelf = request.shelfId() == null ? null : requireShelf(storeId, request.shelfId());
        StoreStocks stock = stockRepository.findByStoreIdAndProductId(storeId, productId).orElse(null);

        if (stock == null) {
            if (shelf == null) {
                return productMapper.toDetail(product, null);     // nothing to remove
            }
            stock = StoreStocks.newRow(storeId, product);           // first time in this store
        }
        stock.assignShelf(shelf);          // checks: active shelf / shelf empty before removing
        stock = stockRepository.save(stock);
        return productMapper.toDetail(product, stock);
    }

    /** POST /api/store/products/{productId}/move  { direction, quantity, note } */
    @Transactional
    public StoreProductDetail moveStock(Long userId, Long productId, MoveStockRequest request) {
        Users user = storeAccessService.requireStoreUser(userId);
        Long storeId = user.getWarehouse().getId();
        Products product = requireActiveProduct(productId);
        StoreStocks stock = requireStock(storeId, productId);

        int quantity = request.quantity();
        StockTransactionType type;
        int shelfChange;
        if (request.direction() == MoveDirection.TO_SHELF) {
            stock.moveToShelf(quantity);
            type = StockTransactionType.MOVE_TO_SHELF;
            shelfChange = quantity;
        } else {
            stock.moveToBackroom(quantity);
            type = StockTransactionType.MOVE_TO_BACKROOM;
            shelfChange = -quantity;
        }

        transactionRepository.save(StoreStockTransactions.builder()
                .code(newTransactionCode(type))
                .storeId(storeId)
                .product(product)
                .type(type)
                .shelfChange(shelfChange)
                .backroomChange(-shelfChange)
                .shelfAfter(stock.getShelfQuantity())
                .backroomAfter(stock.getBackroomQuantity())
                .note(normalize(request.note()))
                .createdBy(user)
                .createdAt(LocalDateTime.now())
                .build());

        return productMapper.toDetail(product, stock);   // stock is managed -> saved at commit (@Version checked)
    }

    /** PUT /api/store/products/{productId}/min-shelf – store manager only */
    @Transactional
    public StoreProductDetail changeMinShelf(Long userId, Long productId, MinShelfRequest request) {
        Users manager = storeAccessService.requireStoreManager(userId);
        Long storeId = manager.getWarehouse().getId();
        Products product = requireActiveProduct(productId);
        StoreStocks stock = requireStock(storeId, productId);
        stock.changeMinShelfQuantity(request.minShelfQuantity());
        return productMapper.toDetail(product, stock);
    }

    // ---------- helpers ----------

    private Products requireActiveProduct(Long productId) {
        return productRepository.findByIdAndStatus(productId, ProductStatus.ACTIVE)
                .orElseThrow(() -> new NotFound("Product not found"));
    }

    private StoreShelves requireShelf(Long storeId, Long shelfId) {
        return shelfRepository.findByIdAndStoreId(shelfId, storeId)
                .orElseThrow(() -> new NotFound("Shelf not found"));
    }

    private StoreStocks requireStock(Long storeId, Long productId) {
        return stockRepository.findByStoreIdAndProductId(storeId, productId)
                .orElseThrow(() -> new BadRequest("Product has no stock in this store"));
    }

    /** e.g. STX-MS-3F9A0C1B2D4E5F60 (MS = move to shelf, MB = move to backroom) */
    private static String newTransactionCode(StockTransactionType type) {
        String prefix = type == StockTransactionType.MOVE_TO_SHELF ? "MS" : "MB";
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        return "STX-" + prefix + "-" + random;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}