package com.toystorage.backend.enums.inventory;

public enum StockCountType {

    /*
     * Kiểm kê toàn bộ sản phẩm và vị trí trong kho.
     */
    FULL,

    /*
     * Kiểm kê luân phiên từng khu vực hoặc nhóm sản phẩm.
     */
    CYCLE,

    /*
     * Kiểm kê đột xuất khi phát hiện nghi ngờ
     * hoặc có báo cáo chênh lệch.
     */
    AD_HOC,

    /*
     * Kiểm kê định kỳ giữa tháng.
     */
    MID_MONTH,

    /*
     * Kiểm kê cuối tháng.
     */
    END_OF_MONTH
}