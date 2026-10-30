-- =====================================================
-- ISSUE #6 - CATEGORY MANAGEMENT
-- Add CATEGORY to activity_logs.entity_type
-- =====================================================
-- Already applied to the shared warehouse database.
-- DO NOT run this migration again on the shared DB.
-- For another environment, verify the existing ENUM
-- values and column definition before executing.
-- =====================================================

ALTER TABLE warehouse.activity_logs
MODIFY COLUMN entity_type ENUM(
    'AUTHENTICATION',
    'GOODS_RECEIPT',
    'INVENTORY',
    'INVENTORY_ADJUSTMENT',
    'PACKAGE',
    'PERMISSION',
    'PRODUCT',
    'PURCHASE_ORDER',
    'ROLE',
    'ROLE_PERMISSION',
    'SALES_ORDER',
    'SHIPMENT',
    'STOCK_COUNT',
    'STOCK_TRANSFER',
    'STORE_RECEIPT',
    'STORE_RETURN',
    'SUPPLIER',
    'SUSPICIOUS_ALERT',
    'USER',
    'USER_ROLE',
    'WAREHOUSE',
    'CATEGORY'
)
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci
NULL DEFAULT NULL;
