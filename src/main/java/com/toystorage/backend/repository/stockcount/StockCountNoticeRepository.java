package com.toystorage.backend.repository.stockcount;

import com.toystorage.backend.services.inventories.stockcount.StockCountNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StockCountNoticeRepository extends JpaRepository<StockCountNotice, Long> {
    List<StockCountNotice> findTop30ByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<StockCountNotice> findByIdAndUserId(Long id, Long userId);
    // Thong bao "da den tuan kiem ke" chi tao mot lan cho moi ke hoach / nguoi dung.
    boolean existsByStockCountIdAndUserIdAndCreatedAtGreaterThanEqual(Long stockCountId, Long userId, java.time.LocalDateTime from);
}
