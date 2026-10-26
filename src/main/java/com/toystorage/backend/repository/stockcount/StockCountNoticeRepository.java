package com.toystorage.backend.repository.stockcount;

import com.toystorage.backend.stockcount.StockCountNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StockCountNoticeRepository extends JpaRepository<StockCountNotice, Long> {
    List<StockCountNotice> findTop30ByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<StockCountNotice> findByIdAndUserId(Long id, Long userId);
}
