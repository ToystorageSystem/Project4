-- ============================================================
-- TASK #27 - SUSPICIOUS ACTIVITY ALERT AUDIT SUPPORT
-- ============================================================

ALTER TABLE activity_logs
MODIFY COLUMN entity_type ENUM(
    'USER',
    'ROLE',
    'PERMISSION',
    'USER_ROLE',
    'ROLE_PERMISSION',
    'AUTHENTICATION',
    'WAREHOUSE',
    'PRODUCT',
    'SUPPLIER',
    'PURCHASE_ORDER',
    'GOODS_RECEIPT',
    'INVENTORY',
    'STOCK_TRANSFER',
    'PACKAGE',
    'SHIPMENT',
    'STORE_RECEIPT',
    'STORE_RETURN',
    'SALES_ORDER',
    'STOCK_COUNT',
    'INVENTORY_ADJUSTMENT',
    'SUSPICIOUS_ALERT'
) NULL;