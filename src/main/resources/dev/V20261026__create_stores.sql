-- Backup database first. This migration is designed for MySQL 8.
-- This is a 1:1 extension table, NOT a removal of STORE rows from warehouses.
CREATE TABLE IF NOT EXISTS stores (
    warehouse_id BIGINT NOT NULL PRIMARY KEY,
    store_manager_name VARCHAR(150) NULL,
    opening_hours VARCHAR(100) NULL,
    store_phone VARCHAR(20) NULL,
    CONSTRAINT fk_stores_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

-- Backfill existing STORE-type locations without changing their IDs.
INSERT INTO stores (warehouse_id)
SELECT w.id FROM warehouses w
WHERE w.type = 'STORE'
  AND NOT EXISTS (SELECT 1 FROM stores s WHERE s.warehouse_id = w.id);

-- Validation: must be 0 before application rollout.
SELECT COUNT(*) AS missing_store_extensions
FROM warehouses w LEFT JOIN stores s ON s.warehouse_id = w.id
WHERE w.type = 'STORE' AND s.warehouse_id IS NULL;

-- Validation: must be 0 (DB FK alone cannot enforce type='STORE').
SELECT COUNT(*) AS invalid_store_extensions
FROM stores s JOIN warehouses w ON w.id = s.warehouse_id
WHERE w.type <> 'STORE';
