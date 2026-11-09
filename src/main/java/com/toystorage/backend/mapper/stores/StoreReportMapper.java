package com.toystorage.backend.mapper.stores;

import org.springframework.stereotype.Component;

import com.toystorage.backend.dto.response.stores.StockTransactionLine;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.stores.StoreStockTransactions;

@Component
public class StoreReportMapper {

    public StockTransactionLine toLine(StoreStockTransactions t) {
        Products p = t.getProduct();
        Long receiptId = t.getStoreReceipt() == null ? null : t.getStoreReceipt().getId();
        return new StockTransactionLine(t.getId(), t.getCode(), t.getType(),
                p.getId(), p.getProductsCode(), p.getName(),
                t.getShelfChange(), t.getBackroomChange(), t.getShelfAfter(), t.getBackroomAfter(),
                t.getSaleId(), receiptId, t.getNote(),
                t.getCreatedBy().getName(), t.getCreatedAt());
    }
}
