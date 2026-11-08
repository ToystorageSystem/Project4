-- ============================================================
-- TASK #45 - AI ANALYTICS DASHBOARD
-- Synchronize inventory available quantity
--
-- Rule:
-- available_quantity = quantity - reserved_quantity
--
-- Safe to run multiple times.
-- Only mismatched rows are updated.
-- ============================================================


-- ============================================================
-- 1. VERIFY BEFORE UPDATE
-- ============================================================

SELECT
    id,
    warehouse_id,
    location_id,
    product_id,
    quantity,
    reserved_quantity,
    available_quantity,
    quantity - reserved_quantity AS expected_available,
    available_quantity
        - (quantity - reserved_quantity) AS difference
FROM inventory_balances
WHERE available_quantity
      <> quantity - reserved_quantity
ORDER BY id;


-- ============================================================
-- 2. SYNCHRONIZE DATA
-- ============================================================

START TRANSACTION;

UPDATE inventory_balances
SET
    available_quantity =
        quantity - reserved_quantity,

    updated_at =
        NOW(6)
WHERE available_quantity
      <> quantity - reserved_quantity;


-- Number of corrected rows.
SELECT
    ROW_COUNT() AS corrected_rows;

COMMIT;


-- ============================================================
-- 3. VERIFY AFTER UPDATE
-- ============================================================

SELECT
    COUNT(*) AS mismatch_rows_after_update
FROM inventory_balances
WHERE available_quantity
      <> quantity - reserved_quantity;


SELECT
    id,
    warehouse_id,
    location_id,
    product_id,
    quantity,
    reserved_quantity,
    available_quantity,
    quantity - reserved_quantity AS expected_available
FROM inventory_balances
ORDER BY warehouse_id, id;