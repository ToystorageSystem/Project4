package com.toystorage.backend.stockcount;

import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.warehouses.WarehouseType;
import com.toystorage.backend.repository.stockcount.StockCountWarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.YearMonth;

@Configuration
@EnableScheduling
class StockCountSchedulingConfiguration {}

@Component
@RequiredArgsConstructor
public class StockCountScheduler {
    private static final Logger log = LoggerFactory.getLogger(StockCountScheduler.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final StockCountPeriodicCreator creator;
    private final StockCountWarehouseRepository warehouses;
    @Value("${app.stock-count.scheduler.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void daily() { runFor(LocalDate.now(ZONE)); }

    // Reconcile if the application was restarted after midnight on the scheduled date.
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() { runFor(LocalDate.now(ZONE)); }

    public void runFor(LocalDate date) {
        if (!enabled) return;
        // Prepare the current and following month so plans appear in Upcoming
        // before the first permitted workday. This is idempotent on restart.
        for (int monthOffset = 0; monthOffset < 2; monthOffset++) {
            YearMonth month = YearMonth.from(date).plusMonths(monthOffset);
            for (StockCountWindowPolicy.Window window : StockCountWindowPolicy.windows(month)) {
                if (window.end().isBefore(date)) continue;
                for (Warehouses warehouse : warehouses.findByType(WarehouseType.MAIN_WAREHOUSE)) {
                    if (warehouse.getStatus() != com.toystorage.backend.enums.warehouses.WarehouseStatus.ACTIVE) continue;
                    try {
                        boolean created = creator.ensure(warehouse, window.start(), window.type());
                        if (created) log.info("Stock count prepared for warehouse {}: {} to {}",
                                warehouse.getId(), window.start(), window.end());
                    } catch (org.springframework.dao.DataIntegrityViolationException ignored) {
                        log.info("Concurrent stock count creation skipped: {} {}", warehouse.getId(), window.start());
                    } catch (Exception ex) {
                        log.error("Cannot prepare stock count for warehouse {} during {} - {}",
                                warehouse.getId(), window.start(), window.end(), ex);
                    }
                }
            }
        }
    }
}
