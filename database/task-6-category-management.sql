-- =========================================================
-- TASK #6 - CATEGORY MANAGEMENT
-- =========================================================
-- This patch must be applied once before running the backend
-- version that contains category management.
--
-- Hibernate is configured with:
-- spring.jpa.hibernate.ddl-auto=validate
--
-- Therefore schema changes are NOT applied automatically.
-- =========================================================


-- =========================================================
-- 1. Add optional category description
-- =========================================================

ALTER TABLE categories
ADD COLUMN description TEXT NULL AFTER name;


-- =========================================================
-- 2. Allow CATEGORY in activity_logs.entity_type
-- =========================================================

ALTER TABLE activity_logs
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
    'USER',
    'USER_ROLE',
    'WAREHOUSE',
    'CATEGORY'
) NULL;