package com.toystorage.backend.mapper.stores;

import org.springframework.stereotype.Component;

import com.toystorage.backend.dto.response.stores.ShelfInfo;
import com.toystorage.backend.dto.response.stores.StoreProductDetail;
import com.toystorage.backend.dto.response.stores.StoreProductSummary;
import com.toystorage.backend.entity.products.Brands;
import com.toystorage.backend.entity.products.Products;
import com.toystorage.backend.entity.stores.StoreShelves;
import com.toystorage.backend.entity.stores.StoreStocks;

/**
 * Products + StoreStocks -> store DTOs.
 * stock can be null: the store has no stock row for this product yet
 * -> shelf null, all quantities 0.
 */
@Component
public class StoreProductMapper {

    public ShelfInfo toShelfInfo(StoreShelves shelf) {
        if (shelf == null) {
            return null;
        }
        return new ShelfInfo(shelf.getId(), shelf.getCode(), shelf.getName());
    }

    public StoreProductSummary toSummary(Products product, StoreStocks stock) {
        Brands brand = product.getBrand();
        return new StoreProductSummary(
                product.getId(),
                product.getProductsCode(),
                product.getBarcode(),
                product.getName(),
                null,                               // TODO: Products has no imageUrl yet
                product.getSellingPrice(),
                product.getBaseUnit(),
                product.getCategory().getName(),
                brand == null ? null : brand.getName(),
                stock == null ? null : toShelfInfo(stock.getShelf()),
                stock == null ? 0 : stock.getShelfQuantity(),
                stock == null ? 0 : stock.getTotalQuantity());
    }

    public StoreProductDetail toDetail(Products product, StoreStocks stock) {
        Brands brand = product.getBrand();

        ShelfInfo shelf = stock == null ? null : toShelfInfo(stock.getShelf());
        int shelfQty = stock == null ? 0 : stock.getShelfQuantity();
        int backroomQty = stock == null ? 0 : stock.getBackroomQuantity();
        int minShelfQty = stock == null ? 0 : stock.getMinShelfQuantity();

        return new StoreProductDetail(
                product.getId(),
                product.getProductsCode(),
                product.getBarcode(),
                product.getName(),
                null,                               // TODO: Products has no imageUrl yet
                product.getSellingPrice(),
                product.getBaseUnit(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                brand == null ? null : brand.getId(),
                brand == null ? null : brand.getName(),
                brand == null ? null : brand.getCountry(),
                shelf,
                shelfQty,
                backroomQty,
                shelfQty + backroomQty,
                minShelfQty,
                shelf != null && shelfQty <= minShelfQty,   // lowStock
                stock == null ? null : stock.getUpdatedAt());
    }
}