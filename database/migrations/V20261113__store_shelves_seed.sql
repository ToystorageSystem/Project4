-- ToyStorage – demo shelves for the stores of the shared seed (2026-11-13).
-- Run once after ToyStorage-Full-Test-Seed-Corrected-Manager.sql. Safe to run again (skips existing codes).
-- The table store_shelves is created by Hibernate (ddl-auto=update) when the backend starts.
-- Store Managers can now create / rename / deactivate shelves themselves (Store web > Shelves).

INSERT INTO store_shelves (store_shelves_code, store_id, name, description, status, created_at, updated_at)
SELECT v.code, v.store_id, v.name, v.description, 'ACTIVE', NOW(), NOW()
FROM (
    SELECT 'SHF-10-001' AS code, 10 AS store_id, 'Shelf A - Building toys' AS name, 'Front left, near the entrance' AS description
    UNION ALL SELECT 'SHF-10-002', 10, 'Shelf B - Dolls & plush', 'Middle aisle'
    UNION ALL SELECT 'SHF-10-003', 10, 'Shelf C - Vehicles', 'Back wall'
    UNION ALL SELECT 'SHF-11-001', 11, 'Shelf A - Building toys', 'Front left, near the entrance'
    UNION ALL SELECT 'SHF-11-002', 11, 'Shelf B - Dolls & plush', 'Middle aisle'
) v
JOIN warehouses w ON w.id = v.store_id AND w.type = 'STORE'
WHERE NOT EXISTS (SELECT 1 FROM store_shelves s WHERE s.store_shelves_code = v.code);
