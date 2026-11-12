package com.toystorage.backend.mapper.stores;

import org.springframework.stereotype.Component;

import com.toystorage.backend.dto.response.stores.SaleDetail;
import com.toystorage.backend.dto.response.stores.SaleItemLine;
import com.toystorage.backend.dto.response.stores.SaleSummary;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.stores.StoreSaleItems;
import com.toystorage.backend.entity.stores.StoreSales;

@Component
public class StoreSaleMapper {

    public SaleSummary toSummary(StoreSales s) {
        return new SaleSummary(s.getId(), s.getCode(), s.getTotalQuantity(), s.getTotalAmount(),
                s.getCreatedBy().getName(), s.getCreatedAt());
    }

    public SaleDetail toDetail(StoreSales s) {
        return new SaleDetail(s.getId(), s.getCode(), s.getTotalQuantity(), s.getTotalAmount(),
                s.getNote(), s.getCreatedBy().getName(), s.getCreatedAt(),
                s.getItems().stream().map(this::toLine).toList());
    }

    private SaleItemLine toLine(StoreSaleItems i) {
        Products p = i.getProduct();
        return new SaleItemLine(p.getId(), p.getProductsCode(), p.getBarcode(), p.getName(),
                i.getQuantity(), i.getUnitPrice(), i.getLineTotal(),
                i.getFromShelf(), i.getFromBackroom());
    }
}