-- Business Operations / Purchase Order delivery-date migration.
-- Safe to run more than once on the shared warehouse database.
USE warehouse;

-- DRAFT already allows NULL in the existing expected_delivery_date column.
-- Keep it as the original requested date even after supplier feedback.

SET @has_proposed = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'purchase_orders'
      AND COLUMN_NAME = 'supplier_proposed_delivery_date'
);
SET @sql = IF(@has_proposed = 0,
    'ALTER TABLE purchase_orders ADD COLUMN supplier_proposed_delivery_date DATE NULL',
    'SELECT ''supplier_proposed_delivery_date already exists'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_agreed = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'purchase_orders'
      AND COLUMN_NAME = 'agreed_delivery_date'
);
SET @sql = IF(@has_agreed = 0,
    'ALTER TABLE purchase_orders ADD COLUMN agreed_delivery_date DATE NULL',
    'SELECT ''agreed_delivery_date already exists'' AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Both new columns must exist and old orders remain unchanged.
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'purchase_orders'
  AND COLUMN_NAME IN (
      'expected_delivery_date',
      'supplier_proposed_delivery_date',
      'agreed_delivery_date'
  )
ORDER BY COLUMN_NAME;
