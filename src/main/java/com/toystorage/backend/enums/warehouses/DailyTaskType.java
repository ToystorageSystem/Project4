package com.toystorage.backend.enums.warehouses;

/** Loai cong viec Warehouse Manager giao cho nhan vien trong ngay. */
public enum DailyTaskType {
    RECEIVING,        // Nhan hang / kiem hang nhap
    PUTAWAY,          // Cat hang len vi tri
    ARRANGEMENT,      // Sap xep, don dep, dieu chinh vi tri hang hoa
    PICKING,          // Lay hang cho phieu dieu chuyen / xuat
    PACKING,          // Dong goi, niem phong
    DISPATCH,         // Ban giao xuat kho
    TRANSFER,         // Dieu chuyen hang giua kho / cua hang
    STOCK_COUNT,      // Kiem ke
    DAMAGE_HANDLING,  // Xu ly hang hong
    SHORTAGE_HANDLING,// Xu ly hang thieu / chenh lech
    STORE_RETURN,     // Nhan hang tra ve tu cua hang
    OTHER
}
