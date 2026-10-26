package com.toystorage.backend.stockcount;

import com.toystorage.backend.entity.inventories.StockCounts;
import com.toystorage.backend.entity.users.Users;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.inventories.StockCountType;
import com.toystorage.backend.enums.inventories.StockCountStatus;
import com.toystorage.backend.repository.stockcount.StockCountLockedRepository;
import com.toystorage.backend.repository.stockcount.StockCountNoticeRepository;
import com.toystorage.backend.repository.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockCountPeriodicCreator {
    private final StockCountLockedRepository plans;
    private final StockCountNoticeRepository notices;
    private final UserRepository users;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean ensure(Warehouses warehouse, LocalDate date, StockCountType type) {
        // Also detect legacy plans whose scheduled_date is 15th or 30th/31st.
        // A warehouse may have at most one periodic plan of each type per month.
        YearMonth periodMonth = YearMonth.from(date);
        if (plans.findByWarehouseIdOrderByIdDesc(warehouse.getId()).stream().anyMatch(existing ->
                existing.getCountType() == type && existing.getScheduledDate() != null
                        && YearMonth.from(existing.getScheduledDate()).equals(periodMonth))) return false;
        List<Users> managers = users.findActiveUsersByRoleCode("WAREHOUSE_MANAGER").stream()
                .filter(u -> u.getWarehouse() != null && warehouse.getId().equals(u.getWarehouse().getId()))
                .toList();
        if (managers.isEmpty()) return false; // Cannot satisfy created_by NOT NULL; log is emitted by caller.

        String code = "SC-" + date.toString().replace("-", "") + "-W" + warehouse.getId() +
                (type == StockCountType.MID_MONTH ? "-MID" : "-EOM");
        StockCounts c = new StockCounts();
        c.setCountCode(code); c.setStockCountsCode(code); c.setPeriodicKey(warehouse.getId() + ":" + date);
        c.setWarehouse(warehouse); c.setCountType(type); c.setScheduledDate(date);
        c.setStatus(StockCountStatus.PLANNED); c.setScopeType("ALL");
        c.setCreatedBy(managers.get(0)); plans.saveAndFlush(c);
        for (Users manager : managers) {
            StockCountNotice notice = new StockCountNotice();
            notice.setStockCount(c); notice.setUser(manager); notices.save(notice);
        }
        return true;
    }
}
