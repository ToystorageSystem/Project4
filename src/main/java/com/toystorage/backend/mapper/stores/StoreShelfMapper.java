package com.toystorage.backend.mapper.stores;

import java.util.List;

import org.springframework.stereotype.Component;

import com.toystorage.backend.dto.response.stores.LowShelfItem;
import com.toystorage.backend.dto.response.stores.ShelfDetail;
import com.toystorage.backend.dto.response.stores.ShelfInfo;
import com.toystorage.backend.dto.response.stores.ShelfProductLine;
import com.toystorage.backend.dto.response.stores.ShelfSummary;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.stores.StoreShelves;
import com.toystorage.backend.entity.stores.StoreStocks;
import com.toystorage.backend.repository.stores.ShelfUsage;

@Component
public class StoreShelfMapper {

    /** usage = null -> empty shelf */
    public ShelfSummary toSummary(StoreShelves shelf, ShelfUsage usage) {
        long productCount = usage == null ? 0 : usage.productCount();
        long shelfQuantity = usage == null ? 0 : usage.shelfQuantity();
        return new ShelfSummary(shelf.getId(), shelf.getCode(), shelf.getName(),
                shelf.getDescription(), productCount, shelfQuantity);
    }

    public ShelfDetail toDetail(StoreShelves shelf, List<StoreStocks> stocks) {
        List<ShelfProductLine> lines = stocks.stream().map(this::toLine).toList();
        return new ShelfDetail(shelf.getId(), shelf.getCode(), shelf.getName(),
                shelf.getDescription(), lines);
    }

    public ShelfProductLine toLine(StoreStocks s) {
        Products p = s.getProduct();
        return new ShelfProductLine(p.getId(), p.getProductsCode(), p.getBarcode(), p.getName(),
                s.getShelfQuantity(), s.getBackroomQuantity(), s.getMinShelfQuantity(),
                s.isLowStock());
    }

    public LowShelfItem toLowShelf(StoreStocks s) {
        Products p = s.getProduct();
        StoreShelves sh = s.getShelf();
        return new LowShelfItem(p.getId(), p.getProductsCode(), p.getBarcode(), p.getName(),
                new ShelfInfo(sh.getId(), sh.getCode(), sh.getName()),
                s.getShelfQuantity(), s.getBackroomQuantity(), s.getMinShelfQuantity());
    }
}