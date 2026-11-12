package com.toystorage.backend.services.inventories.report;

import com.toystorage.backend.entity.inventories.InventoryBalances;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.warehouses.Warehouses;
import com.toystorage.backend.enums.warehouses.WarehouseStatus;
import com.toystorage.backend.enums.warehouses.WarehouseType;
import com.toystorage.backend.repository.inventories.InventoryBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gui email tong hop ton kho thap moi ngay cho bo phan kinh doanh.
 * Mac dinh tat, chi hoat dong khi duoc cau hinh SMTP va nguoi nhan.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.low-stock-mail.enabled", havingValue = "true")
public class StoreLowStockMailScheduler {

    private static final Logger log = LoggerFactory.getLogger(StoreLowStockMailScheduler.class);

    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final JavaMailSender mailSender;

    @Value("${app.low-stock-mail.to:}")
    private String recipientEmails;

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional(readOnly = true)
    public void sendDailyLowStockSummary() {
        if (recipientEmails == null || recipientEmails.isBlank()) {
            log.warn("Store low-stock email is enabled but no recipient is configured");
            return;
        }

        Map<String, StoreProductBalance> grouped = new HashMap<>();
        for (InventoryBalances balance : inventoryBalanceRepository.findAll()) {
            if (balance == null) continue;
            Warehouses store = balance.getWarehouse();
            Products product = balance.getProduct();
            if (store == null || product == null
                    || store.getType() != WarehouseType.STORE
                    || store.getStatus() != WarehouseStatus.ACTIVE) continue;

            String key = store.getId() + ":" + product.getId();
            StoreProductBalance item = grouped.computeIfAbsent(key,
                    ignored -> new StoreProductBalance(store.getName(), product.getName()));
            item.available += balance.getAvailableQuantity() == null ? 0 : balance.getAvailableQuantity();
            item.minimum += Math.max(0, balance.getMinimumStockLevel() == null ? 0 : balance.getMinimumStockLevel());
        }

        List<StoreProductBalance> lowStock = new ArrayList<>(grouped.values());
        lowStock.removeIf(item -> item.available > item.minimum);
        if (lowStock.isEmpty()) return;
        lowStock.sort((a, b) -> {
            int byStore = a.store.compareToIgnoreCase(b.store);
            return byStore != 0 ? byStore : a.product.compareToIgnoreCase(b.product);
        });

        StringBuilder body = new StringBuilder("ToyStorage - Store Low-Stock Alerts\n\n");
        body.append("The following store products are at or below their minimum stock levels:\n\n");
        for (StoreProductBalance item : lowStock) {
            body.append("Store: ").append(item.store)
                    .append(" | Product: ").append(item.product)
                    .append(" | Available: ").append(item.available)
                    .append(" | Minimum: ").append(item.minimum).append('\n');
        }
        body.append("\nPlease check inventory and plan replenishment.\n");

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(recipientEmails.split("\\s*,\\s*"));
        mail.setSubject("ToyStorage - Daily Store Low-Stock Alerts");
        mail.setText(body.toString());
        try {
            mailSender.send(mail);
            log.info("Sent store low-stock email for {} product/store pairs", lowStock.size());
        } catch (RuntimeException e) {
            log.error("Unable to send store low-stock alert email", e);
        }
    }

    private static final class StoreProductBalance {
        private final String store;
        private final String product;
        private int available;
        private int minimum;
        private StoreProductBalance(String store, String product) {
            this.store = store;
            this.product = product;
        }
    }
}
