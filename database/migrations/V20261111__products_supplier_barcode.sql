-- ToyStorage Business Operations - SAME BARCODE ALLOWED FOR DIFFERENT SUPPLIERS
-- MySQL 8; BACK UP the database BEFORE executing. DDL commits automatically.
-- Execute each step ONCE on the shared database after testing in a cloned database.
-- Existing orders, inventory and Goods Receipts are NOT merged by this migration.

-- Preflight: this query must return ZERO rows. Resolve any duplicates first.
SELECT sp.supplier_id, p.barcode, COUNT(*) AS duplicate_count
FROM supplier_products sp JOIN products p ON p.id = sp.product_id
GROUP BY sp.supplier_id, p.barcode
HAVING COUNT(*) > 1;

-- Each new Business Product belongs to one supplier.
-- Legacy products without a link, or with many suppliers, may remain NULL.
ALTER TABLE products ADD COLUMN supplier_id BIGINT NULL;

UPDATE products p
JOIN (
    SELECT product_id, MIN(supplier_id) AS supplier_id
    FROM supplier_products
    GROUP BY product_id
    HAVING COUNT(DISTINCT supplier_id) = 1
) single_supplier ON single_supplier.product_id = p.id
SET p.supplier_id = single_supplier.supplier_id
WHERE p.supplier_id IS NULL;

-- Review rows that still have no supplier after backfilling. These remain legacy-only.
SELECT id, products_code, barcode FROM products WHERE supplier_id IS NULL;

ALTER TABLE products
    ADD CONSTRAINT fk_products_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id);

-- Keep Product Code globally unique, but allow a Barcode in DIFFERENT suppliers.
-- The original index normally has this name (confirm with SHOW INDEX FROM products).
ALTER TABLE products DROP INDEX uk_products_barcode;
ALTER TABLE products ADD UNIQUE KEY uk_products_supplier_barcode (supplier_id, barcode);
CREATE INDEX idx_products_barcode ON products (barcode);

-- Verify the index and ownership after migration:
SHOW INDEX FROM products;
SELECT id, products_code, barcode, supplier_id FROM products ORDER BY id DESC LIMIT 20;
