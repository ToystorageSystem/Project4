package com.toystorage.backend.enums.stores;

/** store_stock_transactions.type – must match the MySQL enum exactly */
public enum StockTransactionType {
    RECEIVE,
    MOVE_TO_SHELF,
    MOVE_TO_BACKROOM,
    SALE,
    ADJUST
}