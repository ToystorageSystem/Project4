package com.toystorage.backend.repository.stockcount;

import com.toystorage.backend.stockcount.StockCountAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StockCountAuditRepository extends JpaRepository<StockCountAudit, Long> {
    List<StockCountAudit> findByStockCountIdOrderByCreatedAtAscIdAsc(Long stockCountId);
}
