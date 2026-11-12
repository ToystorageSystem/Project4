package com.toystorage.backend.enums.warehouses;

/**
 * Vai tro trong ca do Warehouse Manager phan cho nhan vien moi ngay.
 * Nhan vien tu nhan viec thuoc vai tro cua minh.
 */
public enum WorkRole {
    RECEIVING,   // Nhan & kiem hang (va cat hang len ke hang minh da kiem)
    OUTBOUND,    // Lay hang, dong goi, ban giao xuat kho
    ISSUES,      // Hang hong, hang thieu, sap xep ke
    STOCK_COUNT, // Kiem ke ton kho thuc te (tuan dau thang, tuan giua thang)
    FLEX         // Ho tro: duoc nhan moi loai viec
}
