package com.toystorage.backend.services.stores;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toystorage.backend.dto.response.common.PageResponse;
import com.toystorage.backend.dto.response.receipts.store.ManifestSummaryResponse;
import com.toystorage.backend.dto.response.stores.DashboardResponse;
import com.toystorage.backend.dto.response.stores.SaleSummary;
import com.toystorage.backend.dto.response.stores.StockTransactionLine;
import com.toystorage.backend.enums.products.ProductStatus;
import com.toystorage.backend.enums.stores.StockTransactionType;
import com.toystorage.backend.exceptions.BadRequest;
import com.toystorage.backend.mapper.stores.StoreReportMapper;
import com.toystorage.backend.mapper.stores.StoreSaleMapper;
import com.toystorage.backend.repository.receipts.store.StoreStockRepository;
import com.toystorage.backend.repository.receipts.store.StoreStockTransactionRepository;
import com.toystorage.backend.repository.stores.SalesTotals;
import com.toystorage.backend.repository.stores.StoreSaleRepository;
import com.toystorage.backend.services.receipts.store.StoreReceivingService;

import lombok.RequiredArgsConstructor;

/** Read-only numbers for the manager web: dashboard + stock history. */
@Service
@RequiredArgsConstructor
public class StoreReportService {

    public static final int MAX_PAGE_SIZE = 50;
    private static final int RECENT_SALES = 5;
    private static final Set<String> ARRIVED_DELIVERY_STATUSES = Set.of("ARRIVED", "DELIVERED");

    private final StoreAccessService storeAccessService;
    private final StoreSaleRepository saleRepository;
    private final StoreStockRepository stockRepository;
    private final StoreStockTransactionRepository transactionRepository;
    private final StoreReceivingService receivingService;
    private final StoreSaleMapper saleMapper;
    private final StoreReportMapper reportMapper;

    /** GET /api/store/dashboard */
    @Transactional(readOnly = true)
    public DashboardResponse dashboard(Long userId) {
        Long storeId = storeAccessService.requireStoreId(userId);

        // today's sales
        LocalDate today = LocalDate.now();
        DateRange todayRange = DateRange.ofDays(today, today);
        SalesTotals totals = saleRepository.sumBetween(storeId, todayRange.from(), todayRange.to());

        // stock alerts
        long lowShelf = stockRepository.countLowShelf(storeId, ProductStatus.ACTIVE);
        long outOfStock = stockRepository.countOutOfStock(storeId, ProductStatus.ACTIVE);

        // manifests (reuse the receiving list: only not COMPLETED)
        long inTransit = 0, toReceive = 0, toInspect = 0, toConfirm = 0;
        for (ManifestSummaryResponse m : receivingService.listManifests(userId)) {
            if (!ARRIVED_DELIVERY_STATUSES.contains(m.deliveryStatus())) {
                inTransit++;
                continue;
            }
            switch (m.receivingStatus()) {
                case NOT_STARTED, RECEIVING -> toReceive++;
                case PACKAGES_RECEIVED -> toInspect++;
                case INSPECTED -> toConfirm++;
                default -> { }
            }
        }

        List<SaleSummary> recent = saleRepository
                .findByStoreIdOrderByCreatedAtDescIdDesc(storeId, PageRequest.of(0, RECENT_SALES))
                .map(saleMapper::toSummary)
                .getContent();

        return new DashboardResponse(today,
                totals.safeSalesCount(), totals.safeItemsSold(), totals.safeRevenue(),
                lowShelf, outOfStock,
                inTransit, toReceive, toInspect, toConfirm,
                recent);
    }

    /** GET /api/store/stock-transactions?productId=&type=&from=&to=&page=&size= */
    @Transactional(readOnly = true)
    public PageResponse<StockTransactionLine> stockTransactions(Long userId, Long productId,
                                                                StockTransactionType type,
                                                                LocalDate from, LocalDate to,
                                                                int page, int size) {
        Long storeId = storeAccessService.requireStoreId(userId);
        if (page < 0) {
            throw new BadRequest("Page must be >= 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequest("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        DateRange range = DateRange.ofDays(from, to);
        return PageResponse.from(transactionRepository
                .search(storeId, productId, type, range.from(), range.to(), PageRequest.of(page, size))
                .map(reportMapper::toLine));
    }
}
