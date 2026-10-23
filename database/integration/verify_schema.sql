-- READ-ONLY schema inspection for integration/backend.
-- Run against a copied/test database. This script NEVER changes data or schema.
-- Do not execute the historical SQL migrations automatically against shared MySQL.

SELECT required.table_name,
       required.column_name,
       CASE WHEN actual.column_name IS NULL THEN 'MISSING' ELSE 'PRESENT' END AS column_status,
       actual.column_type
FROM (
    SELECT 'products' table_name, 'image_url' column_name UNION ALL
    SELECT 'products', 'purchase_price' UNION ALL
    SELECT 'categories', 'description' UNION ALL
    SELECT 'inventory_balances', 'minimum_stock_level' UNION ALL
    SELECT 'supplier_invoices', 'due_date' UNION ALL
    SELECT 'stock_transfers', 'expected_shipment_date' UNION ALL
    SELECT 'stock_transfers', 'expected_receipt_date'
) required
LEFT JOIN information_schema.columns actual
  ON actual.table_schema = DATABASE()
 AND actual.table_name = required.table_name
 AND actual.column_name = required.column_name
ORDER BY required.table_name, required.column_name;

-- Check native MySQL ENUMs, if used for these two columns.
-- VARCHAR columns can store these values without a DDL enum change.
SELECT 'products.status' AS field_name,
       c.column_type,
       CASE WHEN c.column_name IS NULL THEN 'MISSING_COLUMN'
            WHEN c.data_type = 'enum'
             AND (LOCATE('''PENDING_CREATE''', c.column_type) = 0
               OR LOCATE('''PENDING_UPDATE''', c.column_type) = 0)
            THEN 'MISSING_ENUM_VALUES'
            ELSE 'OK' END AS integration_status
FROM information_schema.columns c
WHERE c.table_schema = DATABASE()
  AND c.table_name = 'products' AND c.column_name = 'status';

SELECT 'activity_logs.entity_type' AS field_name,
       c.column_type,
       CASE WHEN c.data_type = 'enum'
             AND (LOCATE('''CATEGORY''', c.column_type) = 0
               OR LOCATE('''SUPPLIER_PRODUCT''', c.column_type) = 0
               OR LOCATE('''SUPPLIER_INVOICE''', c.column_type) = 0)
            THEN 'MISSING_ENUM_VALUES'
            ELSE 'OK' END AS integration_status
FROM information_schema.columns c
WHERE c.table_schema = DATABASE()
  AND c.table_name = 'activity_logs' AND c.column_name = 'entity_type';
