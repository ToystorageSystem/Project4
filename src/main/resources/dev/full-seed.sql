SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- MASTER DATA
-- ============================================================

INSERT INTO brands
(id, brands_code, country, created_at, description, name, status, updated_at)
VALUES
(1, 'BR-PAWNOBI', 'Vietnam', NOW(), 'Test brand for dog food', 'Pawnobi', 'ACTIVE', NOW()),
(2, 'BR-SAMIU', 'Vietnam', NOW(), 'Test pet brand', 'Samiu Pet', 'ACTIVE', NOW());

INSERT INTO categories
(id, categories_code, name, description, status)
VALUES
(1, 'CAT-DOG-FOOD', 'Dog Food', 'Dry and wet dog food', 'ACTIVE'),
(2, 'CAT-PET-CARE', 'Pet Care', 'Pet care products', 'ACTIVE');

INSERT INTO permissions
(id, action_name, created_at, description, module_name, permission_code, status, updated_at)
VALUES
(1, 'VIEW', NOW(), 'View receiving data', 'STORE_RETURN', 'STORE_RETURN_VIEW', 'ACTIVE', NOW()),
(2, 'RECEIVE', NOW(), 'Receive store return', 'STORE_RETURN', 'STORE_RETURN_RECEIVE', 'ACTIVE', NOW()),
(3, 'VIEW', NOW(), 'View inventory', 'INVENTORY', 'INVENTORY_VIEW', 'ACTIVE', NOW()),
(4, 'MANAGE', NOW(), 'Manage inventory', 'INVENTORY', 'INVENTORY_MANAGE', 'ACTIVE', NOW()),
(5, 'APPROVE', NOW(), 'Approve purchase orders', 'PURCHASE_ORDER', 'PURCHASE_ORDER_APPROVE', 'ACTIVE', NOW()),
(6, 'VIEW', NOW(), 'View transfers', 'STOCK_TRANSFER', 'STOCK_TRANSFER_VIEW', 'ACTIVE', NOW());

INSERT INTO roles
(id, roles_code, role_name, description, status)
VALUES
(1, 'ROLE-ADMIN', 'ADMIN', 'System administrator', 'ACTIVE'),
(2, 'ROLE-WH-MANAGER', 'WAREHOUSE_MANAGER', 'Warehouse Manager', 'ACTIVE'),
(3, 'ROLE-BIZ-MANAGER', 'BUSINESS_MANAGER', 'Business Manager', 'ACTIVE'),
(4, 'ROLE-WH-STAFF', 'WAREHOUSE_STAFF', 'Warehouse Staff', 'ACTIVE'),
(5, 'ROLE-STORE-MANAGER', 'STORE_MANAGER', 'Store Manager', 'ACTIVE'),
(6, 'ROLE-STORE-STAFF', 'STORE_STAFF', 'Store Staff', 'ACTIVE'),
(7, 'ROLE-DRIVER', 'DRIVER', 'Delivery Driver', 'ACTIVE');

INSERT INTO suppliers
(id, address, contact_person, contact_position, created_at, email, name, note, phone, status, suppliers_code, tax_code, updated_at)
VALUES
(1, 'Ho Chi Minh City', 'Nguyen Van A', 'Sales Manager', NOW(), 'supplier1@example.com', 'Pawnobi Supplier', 'Primary development supplier', '0901000001', 'ACTIVE', 'SUP-000001', 'TAX000001', NOW()),
(2, 'Ha Noi', 'Tran Thi B', 'Account Manager', NOW(), 'supplier2@example.com', 'Pet Food Supplier 2', 'Secondary supplier', '0901000002', 'ACTIVE', 'SUP-000002', 'TAX000002', NOW());

INSERT INTO supplier_performance
(id, accuracy_rate, damaged_count, evaluated_at, on_time_deliveries, shortage_count, supplier_performance_code, total_deliveries, supplier_id)
VALUES
(1, 96.50, 1, NOW(), 18, 2, 'SPF-000001', 20, 1);

-- ============================================================
-- WAREHOUSE / STORE / USERS
-- ============================================================

INSERT INTO warehouses
(id, address, phone, created_at, name, status, type, updated_at, warehouses_code)
VALUES
(9, 'Thu Duc City, Ho Chi Minh City', '0902000009', NOW(), 'Main Warehouse HCM', 'ACTIVE', 'MAIN_WAREHOUSE', NOW(), 'WH-000009'),
(10, 'District 1, Ho Chi Minh City', '0902000010', NOW(), 'Store District 1', 'ACTIVE', 'STORE', NOW(), 'STORE-000010'),
(11, 'District 7, Ho Chi Minh City', '0902000011', NOW(), 'Store District 7', 'ACTIVE', 'STORE', NOW(), 'STORE-000011');

INSERT INTO users
(id, created_at, email, identity_card, name, password, status, updated_at, user_code, warehouse_id, must_change_password, phone)
VALUES
(10, NOW(), 'phuctan053@gmail.com', '000000000008', 'Phúc Tân', 'DEV_PASSWORD_PLACEHOLDER', 'ACTIVE', NOW(), 'DEV002', NULL, 0, '0903000010'),
(11, NOW(), 'quocdatvan97@gmail.com', '000000000009', 'Quốc Đạt', 'DEV_PASSWORD_PLACEHOLDER', 'ACTIVE', NOW(), 'DEV003', 10, 0, '0903000011'),
(12, NOW(), 'dohoangducminh19032004@gmail.com', '000000000007', 'Đỗ Hoàng Đức Minh', 'DEV_PASSWORD_PLACEHOLDER', 'ACTIVE', NOW(), 'DEV001', NULL, 0, '0903000012'),
(13, NOW(), 'dinhlam030805@gmail.com', '000000000010', 'Đinh Lâm', 'DEV_PASSWORD_PLACEHOLDER', 'ACTIVE', NOW(), 'DEV004', 9, 0, '0903000013');

INSERT INTO user_roles
(id, user_roles_code, role_id, user_id)
VALUES
(1, 'UR-DEV002-1', 1, 10), -- Phúc Tân -> ADMIN
(2, 'UR-DEV003-5', 5, 11), -- Quốc Đạt -> STORE_MANAGER
(3, 'UR-DEV001-3', 3, 12), -- Đỗ Hoàng Đức Minh -> BUSINESS_MANAGER
(4, 'UR-DEV004-2', 2, 13); -- Đinh Lâm -> WAREHOUSE_MANAGER

INSERT INTO role_permissions
(id, granted_at, granted_by, permission_id, role_id)
VALUES
(1, NOW(), 10, 1, 2), -- Warehouse Manager: STORE_RETURN_VIEW
(2, NOW(), 10, 2, 2), -- Warehouse Manager: STORE_RETURN_RECEIVE
(3, NOW(), 10, 3, 2), -- Warehouse Manager: INVENTORY_VIEW
(4, NOW(), 10, 4, 2), -- Warehouse Manager: INVENTORY_MANAGE
(5, NOW(), 10, 5, 3), -- Business Manager: PURCHASE_ORDER_APPROVE
(6, NOW(), 10, 6, 5), -- Store Manager: STOCK_TRANSFER_VIEW
(7, NOW(), 10, 1, 1), -- Admin: STORE_RETURN_VIEW
(8, NOW(), 10, 3, 1), -- Admin: INVENTORY_VIEW
(9, NOW(), 10, 4, 1), -- Admin: INVENTORY_MANAGE
(10, NOW(), 10, 5, 1), -- Admin: PURCHASE_ORDER_APPROVE
(11, NOW(), 10, 6, 1); -- Admin: STOCK_TRANSFER_VIEW

INSERT INTO warehouse_locations
(id, location_type, name, shelf, status, warehouse_code, warehouse_locations_code, zone, warehouse_id)
VALUES
(1, 'NORMAL', 'Main Normal Shelf A01', 'SHELF-01', 'ACTIVE', 'A-01-01', 'WL-000001', 'ZONE-A', 9),
(2, 'QUARANTINE', 'Main Quarantine Q01', 'Q-SHELF-01', 'ACTIVE', 'Q-01-01', 'WL-000002', 'ZONE-Q', 9),
(3, 'RETURN', 'Main Return Receiving', 'RETURN-01', 'ACTIVE', 'R-01-01', 'WL-000003', 'ZONE-R', 9),
(4, 'DAMAGED', 'Main Damaged Area', 'DMG-01', 'ACTIVE', 'D-01-01', 'WL-000004', 'ZONE-D', 9),
(5, 'NORMAL', 'Store Q1 Shelf A01', 'SHELF-01', 'ACTIVE', 'S1-A-01', 'WL-000005', 'STORE-A', 10),
(6, 'NORMAL', 'Store Q7 Shelf A01', 'SHELF-01', 'ACTIVE', 'S2-A-01', 'WL-000006', 'STORE-A', 11);

-- ============================================================
-- PRODUCTS / SUPPLIER PRODUCTS
-- ============================================================

INSERT INTO products
(id, barcode, base_unit, created_at, name, products_code, rejection_reason, selling_price, status, updated_at, approved_by, brand_id, category_id, created_by, image_url, purchase_price)
VALUES
(7, '8938500000007', 'BAG', NOW(), 'Pawnobi PAW101 1.5kg', 'PAW101-1500', NULL, 185000.00, 'ACTIVE', NOW(), 12, 1, 1, 12, NULL, 120000.00),
(8, '8938500000008', 'BAG', NOW(), 'Pawnobi PAW101 500g', 'PAW101-500', NULL, 79000.00, 'ACTIVE', NOW(), 12, 1, 1, 12, NULL, 48000.00),
(9, '8938500000009', 'PACK', NOW(), 'Pet Reward Soup', 'SOUP-001', NULL, 25000.00, 'ACTIVE', NOW(), 12, 2, 2, 12, NULL, 12000.00);

INSERT INTO supplier_products
(id, lead_time_days, minimum_order_quantity, purchase_price, status, supplier_products_code, product_id, supplier_id, is_default, supplier_product_code)
VALUES
(1, 3, 10, 120000.00, 'ACTIVE', 'SUPP-PROD-001', 7, 1, 1, 'PNB-PAW101-15'),
(2, 3, 20, 48000.00, 'ACTIVE', 'SUPP-PROD-002', 8, 1, 1, 'PNB-PAW101-05');

INSERT INTO product_change_requests
(id, approved_at, created_at, new_value, old_value, product_change_requests_code, rejection_reason, request_reason, status, approved_by, created_by, product_id)
VALUES
(1, NOW(), DATE_SUB(NOW(), INTERVAL 1 DAY), '{"sellingPrice":189000}', '{"sellingPrice":185000}', 'PCR-000001', NULL, 'Price adjustment test', 'APPROVED', 12, 12, 7);

-- ============================================================
-- PURCHASE ORDER / GOODS RECEIVING
-- ============================================================

INSERT INTO purchase_orders
(id, approved_at, cancel_reason, created_at, expected_delivery_date, note, order_code, rejection_reason, status, supplier_responded_at, supplier_response_note, supplier_response_status, updated_at, approved_by, created_by, supplier_id, warehouse_id)
VALUES
(14, DATE_SUB(NOW(), INTERVAL 5 DAY), NULL, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'Primary PO for receiving test', 'PO-2026-0001', NULL, 'ORDERED', DATE_SUB(NOW(), INTERVAL 4 DAY), 'All items available', 'AVAILABLE', NOW(), 12, 12, 1, 9),
(15, NULL, NULL, NOW(), DATE_ADD(CURDATE(), INTERVAL 7 DAY), 'Draft PO', 'PO-2026-0002', NULL, 'DRAFT', NULL, NULL, 'PENDING_RESPONSE', NOW(), NULL, 12, 2, 9);

INSERT INTO purchase_order_items
(id, confirmed_quantity, confirmed_unit_price, ordered_quantity, purchase_order_items_code, received_quantity, supplier_response_note, supplier_response_status, unit_price, product_id, purchase_order_id)
VALUES
(1, 100, 120000.00, 100, 'POI-000001', 40, 'Confirmed', 'AVAILABLE', 120000.00, 7, 14),
(2, 50, 48000.00, 50, 'POI-000002', 20, 'Confirmed', 'AVAILABLE', 48000.00, 8, 14),
(3, NULL, NULL, 40, 'POI-000003', 0, NULL, 'PENDING', 12000.00, 9, 15);

INSERT INTO goods_receipts
(id, created_at, goods_receipts_code, inspection_confirmed_at, receipt_code, received_at, status, updated_at, confirmed_by, inspection_confirmed_by, purchase_order_id, received_by, warehouse_id)
VALUES
(41, DATE_SUB(NOW(), INTERVAL 5 HOUR), 'GRC-WAIT-001', NULL, 'GR-WAIT-001', NULL, 'CREATED', NOW(), NULL, NULL, 14, NULL, 9),
(42, DATE_SUB(NOW(), INTERVAL 4 HOUR), 'GRC-RECV-001', NULL, 'GR-RECV-001', DATE_SUB(NOW(), INTERVAL 3 HOUR), 'RECEIVING', NOW(), NULL, NULL, 14, 13, 9),
(43, DATE_SUB(NOW(), INTERVAL 3 HOUR), 'GRC-INSP-001', NULL, 'GR-INSP-001', DATE_SUB(NOW(), INTERVAL 2 HOUR), 'INSPECTED', NOW(), NULL, NULL, 14, 13, 9),
(44, DATE_SUB(NOW(), INTERVAL 1 DAY), 'GRC-CONF-001', DATE_SUB(NOW(), INTERVAL 20 HOUR), 'GR-CONF-001', DATE_SUB(NOW(), INTERVAL 22 HOUR), 'CONFIRMED', NOW(), 13, 13, 14, 13, 9),
(45, DATE_SUB(NOW(), INTERVAL 2 DAY), 'GRC-DONE-001', DATE_SUB(NOW(), INTERVAL 40 HOUR), 'GR-DONE-001', DATE_SUB(NOW(), INTERVAL 42 HOUR), 'COMPLETED', NOW(), 13, 13, 14, 13, 9);

INSERT INTO goods_receipt_items
(id, accepted_quantity, actual_quantity, damaged_quantity, expected_quantity, goods_receipt_items_code, shortage_quantity, surplus_quantity, goods_receipt_id, product_id)
VALUES
(1, 0, 0, 0, 20, 'GRI-000001', 0, 0, 41, 7),
(2, 8, 8, 0, 10, 'GRI-000002', 2, 0, 42, 7),
(3, 10, 10, 0, 10, 'GRI-000003', 0, 0, 43, 8),
(4, 15, 15, 0, 15, 'GRI-000004', 0, 0, 44, 7),
(5, 20, 20, 0, 20, 'GRI-000005', 0, 0, 45, 7);

-- ============================================================
-- TASK CLAIMS USED BY RECEIVING / PUTAWAY / RETURNS
-- ============================================================

INSERT INTO warehouse_task_claims
(id, claimed_at, reference_id, released_at, task_type, claimed_by, attempt_no)
VALUES
(1, DATE_SUB(NOW(), INTERVAL 3 HOUR), 42, NULL, 'GOODS_RECEIVING', 13, 1),
(2, DATE_SUB(NOW(), INTERVAL 2 HOUR), 1, DATE_SUB(NOW(), INTERVAL 1 HOUR), 'PUTAWAY', 13, 1),
(3, DATE_SUB(NOW(), INTERVAL 1 HOUR), 32, NULL, 'STORE_RETURN_RECEIVING', 13, 1),
(4, DATE_SUB(NOW(), INTERVAL 1 HOUR), 1, NULL, 'STOCK_COUNT', 13, 1);

INSERT INTO receipt_inspections
(id, actual_quantity, evidence_image, expected_quantity, inspected_at, inspected_result, notes, package_code, receipt_inspections_code, goods_receipt_id, inspected_by, product_id, task_claim_id)
VALUES
(1, 8, NULL, 10, DATE_SUB(NOW(), INTERVAL 2 HOUR), 'SHORTAGE', '2 units missing', 'PKG-RECEIVING-01', 'RI-000001', 42, 13, 7, 1);

INSERT INTO discrepancy_reports
(id, created_at, description, discrepancy_reports_code, discrepancy_type, evidence_image_url, reference_id, reference_type, report_code, resolution_action, resolution_note, resolved_at, responsible_party, reviewed_at, status, updated_at, reported_by, resolved_by, reviewed_by, warehouse_id, product_id)
VALUES
(1, DATE_SUB(NOW(), INTERVAL 2 HOUR), 'Supplier receipt shortage test', 'DRC-000001', 'SHORTAGE', NULL, 43, 'GOODS_RECEIPT', 'DR-000001', NULL, NULL, NULL, 'WAREHOUSE_MANAGER', NULL, 'OPEN', NOW(), 13, NULL, NULL, 9, 7),
(2, DATE_SUB(NOW(), INTERVAL 1 HOUR), 'Stock transfer quantity mismatch', 'DRC-000002', 'QUANTITY_MISMATCH', NULL, 21, 'STOCK_TRANSFER', 'DR-000002', 'RECOUNT', 'Recount requested', NULL, 'WAREHOUSE_MANAGER', NOW(), 'INVESTIGATING', NOW(), 13, NULL, 13, 9, 8);

INSERT INTO discrepancy_items
(id, actual_quantity, difference_quantity, discrepancy_items_code, expected_quantity, discrepancy_report_id, product_id)
VALUES
(1, 8, -2, 'DI-000001', 10, 1, 7),
(2, 12, 2, 'DI-000002', 10, 2, 8);

INSERT INTO receiving_incident_reports
(id, confirmed_at, created_at, manager_note, receiving_incident_reports_code, report_code, source_decision, status, updated_at, warehouse_manager_signed_at, warehouse_staff_signed_at, business_manager_id, confirmed_by, goods_receipt_id, warehouse_id, warehouse_manager_id, warehouse_staff_id, penalty_action)
VALUES
(1, NULL, NOW(), 'Investigate supplier shortage', 'RIR-000001', 'RIRPT-000001', 'WAREHOUSE_ACCEPT', 'DRAFT', NOW(), NULL, NOW(), 12, NULL, 43, 9, 13, 13, 'Track supplier shortage and reconcile quantity');

INSERT INTO receiving_incident_report_items
(id, accepted_quantity, actual_quantity, damaged_quantity, discrepancy_type, expected_quantity, receiving_incident_report_items_code, shortage_quantity, surplus_quantity, product_id, report_id, penalty_note, reason, discrepancy_report_id)
VALUES
(1, 8, 8, 0, 'SHORTAGE', 10, 'RIRI-000001', 2, 0, 7, 1, 'No immediate penalty', 'Physical count below expected', 1);

INSERT INTO receiving_reinspection_requests
(id, allow_same_staff, reason, requested_at, goods_receipt_id, requested_by)
VALUES
(1, 0, 'Manager requested recount for shortage test', NOW(), 43, 13);

-- ============================================================
-- PUTAWAY
-- ============================================================

INSERT INTO putaway_tasks
(id, completed_at, created_at, putaway_tasks_code, status, assigned_to, created_by, goods_receipt_id, warehouse_id)
VALUES
(1, NULL, NOW(), 'PA-000001', 'IN_PROGRESS', 13, 13, 44, 9),
(2, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 'PA-000002', 'COMPLETED', 13, 13, 45, 9);

INSERT INTO putaway_task_items
(id, expected_quantity, putaway_quantity, putaway_task_items_code, status, from_location_id, product_id, putaway_task_id, to_location_id)
VALUES
(1, 15, 5, 'PAI-000001', 'PUTAWAYING', 3, 7, 1, 1),
(2, 20, 20, 'PAI-000002', 'COMPLETED', 3, 7, 2, 1);

-- ============================================================
-- INVENTORY
-- ============================================================

INSERT INTO inventory_balances
(id, available_quantity, inventory_balances_code, quantity, reserved_quantity, updated_at, location_id, product_id, warehouse_id, minimum_stock_level)
VALUES
(1, 120, 'IB-000001', 130, 10, NOW(), 1, 7, 9, 20),
(2, 0, 'IB-000002', 5, 0, NOW(), 2, 7, 9, 0),
(3, 30, 'IB-000003', 35, 5, NOW(), 5, 7, 10, 10),
(4, 20, 'IB-000004', 20, 0, NOW(), 1, 8, 9, 10);

INSERT INTO inventory_transactions
(id, created_at, inventory_transactions_code, quantity_after, quantity_before, quantity_change, reference_id, reference_type, transaction_type, location_id, performed_by, product_id, warehouse_id)
VALUES
(1, DATE_SUB(NOW(), INTERVAL 2 DAY), 'IT-000001', 100, 80, 20, 45, 'GOODS_RECEIPT', 'GOODS_RECEIPT', 1, 13, 7, 9),
(2, DATE_SUB(NOW(), INTERVAL 1 DAY), 'IT-000002', 35, 45, -10, 21, 'STOCK_TRANSFER', 'TRANSFER_OUT', 5, 11, 7, 10),
(3, NOW(), 'IT-000003', 130, 125, 5, 1, 'INVENTORY_ADJUSTMENT', 'ADJUSTMENT_INCREASE', 1, 13, 7, 9);

INSERT INTO stock_counts
(id, completed_at, confirmed_at, count_code, count_type, scheduled_date, started_at, status, stock_counts_code, updated_at, assigned_to, confirmed_by, created_by, warehouse_id)
VALUES
(1, NULL, NULL, 'SC-2026-0001', 'CYCLE', CURDATE(), DATE_SUB(NOW(), INTERVAL 2 HOUR), 'COUNTING', 'SCC-000001', NOW(), 13, NULL, 13, 9),
(2, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 'SC-2026-0002', 'AD_HOC', DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 30 HOUR), 'COMPLETED', 'SCC-000002', NOW(), 13, 13, 13, 9);

INSERT INTO stock_count_items
(id, corrected_at, corrected_quantity, correction_reason, difference_quantity, final_quantity, first_count_quantity, second_count_quantity, stock_count_items_code, system_quantity, corrected_by, counted_by, location_id, product_id, recounted_by, stock_count_id)
VALUES
(1, NULL, NULL, NULL, -2, NULL, 128, NULL, 'SCI-000001', 130, NULL, 13, 1, 7, NULL, 1),
(2, DATE_SUB(NOW(), INTERVAL 1 DAY), 130, 'Confirmed after recount', 0, 130, 129, 130, 'SCI-000002', 130, 13, 13, 1, 7, 13, 2);

INSERT INTO inventory_adjustments
(id, adjustment_code, approved_at, created_at, inventory_adjustments_code, reason, status, updated_at, approved_by, created_by, discrepancy_report_id, warehouse_id)
VALUES
(1, 'ADJ-2026-0001', NOW(), DATE_SUB(NOW(), INTERVAL 1 HOUR), 'IAC-000001', 'Correct inventory after development count', 'COMPLETED', NOW(), 13, 13, 1, 9);

INSERT INTO inventory_adjustment_items
(id, adjustment_quantity, inventory_adjustment_items_code, new_quantity, old_quantity, inventory_adjustment_id, location_id, product_id)
VALUES
(1, 5, 'IAI-000001', 130, 125, 1, 1, 7);

-- ============================================================
-- DAMAGED GOODS
-- ============================================================

INSERT INTO damaged_goods_reports
(id, created_at, damaged_goods_reports_code, description, report_code, resolved_at, reviewed_at, source_id, source_type, status, updated_at, reported_by, reviewed_by, warehouse_id)
VALUES
(1, NOW(), 'DGR-000001', 'Damaged bag found during receiving', 'DG-2026-0001', NULL, NOW(), 43, 'GOODS_RECEIPT', 'INSPECTING', NOW(), 13, 13, 9);

INSERT INTO damaged_goods_items
(id, condition_note, damage_type, damaged_goods_items_code, disposition, evidence_image, quantity, status, damaged_goods_report_id, location_id, product_id, confirmed_quantity)
VALUES
(1, 'Bag torn on one side', 'BROKEN', 'DGI-000001', 'QUARANTINE', NULL, 2, 'INSPECTING', 1, 4, 7, 2);

INSERT INTO damaged_goods_history
(id, action, created_at, note, quantity, item_id, performed_by, report_id)
VALUES
(1, 'REPORTED', NOW(), 'Development damaged goods history', 2, 1, 13, 1);

-- ============================================================
-- STOCK TRANSFER / PACKAGE / SHIPMENT
-- ============================================================

INSERT INTO stock_transfers
(id, confirmed_at, created_at, notes, packing_completed_at, reason_code, reason_note, received_at, rejection_reason, shipped_at, status, stock_transfers_code, transfer_code, transfer_type, updated_at, confirmed_by, created_by, from_warehouse_id, packing_completed_by, to_warehouse_id, expected_receipt_date, expected_shipment_date)
VALUES
(21, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 'Main warehouse to Store Q1', DATE_SUB(NOW(), INTERVAL 30 HOUR), 'REBALANCE', 'Replenish store inventory', DATE_SUB(NOW(), INTERVAL 20 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 24 HOUR), 'COMPLETED', 'STC-000021', 'ST-2026-0021', 'NORMAL', NOW(), 13, 13, 9, 13, 10, DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_SUB(CURDATE(), INTERVAL 2 DAY)),
(22, DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 8 HOUR), 'Store return waiting', DATE_SUB(NOW(), INTERVAL 7 HOUR), 'SLOW_MOVING', 'Slow moving return', NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 HOUR), 'SHIPPED', 'STC-000022', 'ST-SR-WAIT-001', 'STORE_RETURN', NOW(), 11, 11, 10, 11, 9, CURDATE(), DATE_SUB(CURDATE(), INTERVAL 1 DAY)),
(23, DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 7 HOUR), 'Store return inspecting', DATE_SUB(NOW(), INTERVAL 6 HOUR), 'SURPLUS', 'Surplus return', NULL, NULL, DATE_SUB(NOW(), INTERVAL 4 HOUR), 'RECEIVING', 'STC-000023', 'ST-SR-INSPECT-001', 'STORE_RETURN', NOW(), 11, 11, 10, 11, 9, CURDATE(), DATE_SUB(CURDATE(), INTERVAL 1 DAY)),
(24, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 'Completed store return', DATE_SUB(NOW(), INTERVAL 30 HOUR), 'SLOW_MOVING', 'Completed return', DATE_SUB(NOW(), INTERVAL 20 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 24 HOUR), 'COMPLETED', 'STC-000024', 'ST-SR-DONE-001', 'STORE_RETURN', NOW(), 11, 11, 10, 11, 9, DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_SUB(CURDATE(), INTERVAL 2 DAY)),
(25, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 'Completed shortage return', DATE_SUB(NOW(), INTERVAL 30 HOUR), 'OTHER', 'Shortage detected', DATE_SUB(NOW(), INTERVAL 18 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 24 HOUR), 'COMPLETED', 'STC-000025', 'ST-SR-SHORT-001', 'STORE_RETURN', NOW(), 11, 11, 10, 11, 9, DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_SUB(CURDATE(), INTERVAL 2 DAY)),
(26, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 'Completed surplus return', DATE_SUB(NOW(), INTERVAL 30 HOUR), 'OTHER', 'Surplus detected', DATE_SUB(NOW(), INTERVAL 18 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 24 HOUR), 'COMPLETED', 'STC-000026', 'ST-SR-SURPLUS-001', 'STORE_RETURN', NOW(), 11, 11, 10, 11, 9, DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_SUB(CURDATE(), INTERVAL 2 DAY));

INSERT INTO stock_transfer_items
(id, approved_quantity, packed_quantity, picked_quantity, received_quantity, requested_quantity, shipped_quantity, shortage_quantity, stock_transfer_items_code, surplus_quantity, product_id, stock_transfer_id)
VALUES
(1, 20, 20, 20, 20, 20, 20, 0, 'STI-000001', 0, 7, 21),
(2, 10, 10, 10, 0, 10, 10, 0, 'STI-000002', 0, 7, 22),
(3, 12, 12, 12, 5, 12, 12, 0, 'STI-000003', 0, 7, 23),
(4, 15, 15, 15, 15, 15, 15, 0, 'STI-000004', 0, 7, 24),
(5, 10, 10, 10, 8, 10, 10, 2, 'STI-000005', 0, 7, 25),
(6, 10, 10, 10, 12, 10, 10, 0, 'STI-000006', 2, 7, 26);

INSERT INTO packages
(id, created_at, packages_code, packed_at, seal_number, sealed_at, status, updated_at, checked_by, from_warehouse_id, packed_by, to_warehouse_id)
VALUES
(51, DATE_SUB(NOW(), INTERVAL 2 DAY), 'PKG-000051', DATE_SUB(NOW(), INTERVAL 30 HOUR), 'SEAL-0051', DATE_SUB(NOW(), INTERVAL 29 HOUR), 'RECEIVED', NOW(), 11, 9, 13, 10),
(52, DATE_SUB(NOW(), INTERVAL 8 HOUR), 'PKG-000052', DATE_SUB(NOW(), INTERVAL 7 HOUR), 'SEAL-0052', DATE_SUB(NOW(), INTERVAL 7 HOUR), 'SHIPPED', NOW(), NULL, 10, 11, 9);

INSERT INTO package_items
(id, package_items_code, quantity, package_id, product_id)
VALUES
(1, 'PKGI-000001', 20, 51, 7),
(2, 'PKGI-000002', 10, 52, 7);

INSERT INTO package_transfer_items
(id, package_transfer_items_code, package_id, stock_transfer_id)
VALUES
(1, 'PTI-000001', 51, 21),
(2, 'PTI-000002', 52, 22);

INSERT INTO shipment_manifests
(id, approved_at, created_at, manifest_code, note, rejection_reason, status, updated_at, approved_by, created_by, from_warehouse_id, to_warehouse_id)
VALUES
(1, DATE_SUB(NOW(), INTERVAL 30 HOUR), DATE_SUB(NOW(), INTERVAL 2 DAY), 'MAN-2026-0001', 'Warehouse to Store shipment', NULL, 'ISSUED', NOW(), 13, 13, 9, 10),
(2, DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 8 HOUR), 'MAN-2026-0002', 'Store return shipment', NULL, 'ISSUED', NOW(), 11, 11, 10, 9);

INSERT INTO shipment_manifest_packages
(id, manifest_id, package_id)
VALUES
(1, 1, 51),
(2, 2, 52);

INSERT INTO shipment_manifest_transfers
(id, manifest_id, transfer_id)
VALUES
(1, 1, 21),
(2, 2, 22);

INSERT INTO deliveries
(id, accepted_at, created_at, delivered_at, status, expected_delivery_at, expected_pickup_at, handed_over_at, rejected_at, rejection_reason, shipment_code, started_at, updated_at, driver_id, from_warehouse_id, handed_over_by, manifest_id, to_warehouse_id)
VALUES
(1, DATE_SUB(NOW(), INTERVAL 26 HOUR), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 20 HOUR), 'DELIVERED', DATE_SUB(NOW(), INTERVAL 20 HOUR), DATE_SUB(NOW(), INTERVAL 28 HOUR), DATE_SUB(NOW(), INTERVAL 27 HOUR), NULL, NULL, 'SHIP-2026-0001', DATE_SUB(NOW(), INTERVAL 25 HOUR), NOW(), 11, 9, 13, 1, 10),
(2, DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 8 HOUR), NULL, 'IN_TRANSIT', DATE_ADD(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 5 HOUR), NULL, NULL, 'SHIP-2026-0002', DATE_SUB(NOW(), INTERVAL 3 HOUR), NOW(), 11, 10, 11, 2, 9);

INSERT INTO delivery_assignment_history
(id, accepted_at, assigned_at, rejected_at, rejection_reason, delivery_id, driver_id)
VALUES
(1, DATE_SUB(NOW(), INTERVAL 26 HOUR), DATE_SUB(NOW(), INTERVAL 28 HOUR), NULL, NULL, 1, 11),
(2, DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 6 HOUR), NULL, NULL, 2, 11);

INSERT INTO delivery_handovers
(id, completed_at, created_at, note, delivery_id, handed_over_by, received_by)
VALUES
(1, DATE_SUB(NOW(), INTERVAL 27 HOUR), DATE_SUB(NOW(), INTERVAL 28 HOUR), 'Warehouse handed package to driver', 1, 13, 11),
(2, DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 6 HOUR), 'Store handed return package to driver', 2, 11, 11);

INSERT INTO delivery_handover_items
(id, actual_seal_number, condition_status, issue_note, scanned_at, handover_id, package_id)
VALUES
(1, 'SEAL-0051', 'NORMAL', NULL, DATE_SUB(NOW(), INTERVAL 27 HOUR), 1, 51),
(2, 'SEAL-0052', 'NORMAL', NULL, DATE_SUB(NOW(), INTERVAL 5 HOUR), 2, 52);

INSERT INTO delivery_locations
(id, accuracy_meters, heading, latitude, longitude, received_at, recorded_at, shipment_locations_code, speed_mps, shipment_id, driver_id)
VALUES
(1, 8.500, 90.00, 10.7769000, 106.7009000, DATE_SUB(NOW(), INTERVAL 24 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR), 'LOC-000001', 8.200, 1, 11),
(2, 6.200, 180.00, 10.8000000, 106.7200000, NOW(), NOW(), 'LOC-000002', 7.500, 2, 11);

INSERT INTO delivery_packages
(id, shipment_packages_code, shipment_id, package_id)
VALUES
(1, 'DP-000001', 1, 51),
(2, 'DP-000002', 2, 52);

-- ============================================================
-- STORE RECEIPT
-- ============================================================

INSERT INTO store_receipts
(id, created_at, received_at, status, store_receipts_code, updated_at, confirmed_by, received_by, stock_transfer_id, store_id)
VALUES
(1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 20 HOUR), 'COMPLETED', 'SRCP-000001', NOW(), 11, 11, 21, 10);

INSERT INTO store_receipt_items
(id, actual_quantity, damaged_quantity, expected_quantity, shortage_quantity, store_receipt_items_code, surplus_quantity, product_id, store_receipt_id)
VALUES
(1, 20, 0, 20, 0, 'SRCPI-000001', 0, 7, 1);

INSERT INTO package_receiving_checks
(id, checked_at, inspection_method, notes, package_condition, package_receiving_checks_code, seal_status, checked_by, package_id, store_receipt_id)
VALUES
(1, DATE_SUB(NOW(), INTERVAL 20 HOUR), 'FULL_COUNT', 'Package checked successfully', 'GOOD', 'PRC-000001', 'INTACT', 11, 51, 1);

-- ============================================================
-- STORE RETURN
-- ============================================================

INSERT INTO store_returns
(id, approved_at, created_at, inspection_submitted_at, issued_at, reason, received_at, return_code, return_type, status, store_returns_code, updated_at, approved_by, created_by, inspected_by, issued_by, stock_transfer_id, store_id, warehouse_id)
VALUES
(31, DATE_SUB(NOW(), INTERVAL 7 HOUR), DATE_SUB(NOW(), INTERVAL 8 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 5 HOUR), 'Slow moving stock return', NULL, 'SR-TEST-WAIT-001', 'SLOW_MOVING', 'SHIPPED', 'SRC-TEST-WAIT-001', NOW(), 11, 11, NULL, 11, 22, 10, 9),
(32, DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 7 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 4 HOUR), 'Surplus stock return', NULL, 'SR-TEST-INSPECT-001', 'SURPLUS', 'INSPECTING', 'SRC-TEST-INSPECT-001', NOW(), 11, 11, 13, 11, 23, 10, 9),
(33, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 20 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR), 'Normal completed return', DATE_SUB(NOW(), INTERVAL 20 HOUR), 'SR-TEST-OK-001', 'SLOW_MOVING', 'RECEIVED', 'SRC-TEST-OK-001', NOW(), 11, 11, 13, 11, 24, 10, 9),
(34, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 18 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR), 'Completed return with shortage', DATE_SUB(NOW(), INTERVAL 18 HOUR), 'SR-TEST-SHORT-001', 'OTHER', 'RECEIVED', 'SRC-TEST-SHORT-001', NOW(), 11, 11, 13, 11, 25, 10, 9),
(35, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 18 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR), 'Completed return with surplus', DATE_SUB(NOW(), INTERVAL 18 HOUR), 'SR-TEST-SURPLUS-001', 'OTHER', 'RECEIVED', 'SRC-TEST-SURPLUS-001', NOW(), 11, 11, 13, 11, 26, 10, 9);

INSERT INTO store_return_items
(id, approved_quantity, condition_status, evidence_image_url, issued_quantity, note, received_quantity, rejected_quantity, requested_quantity, store_return_items_code, from_location_id, product_id, store_return_id, inspected)
VALUES
(1, 10, 'NORMAL', NULL, 10, 'Waiting for warehouse inspection', 0, 0, 10, 'SRI-WAIT-001', 5, 7, 31, 0),
(2, 12, 'NORMAL', NULL, 12, 'Warehouse inspection in progress', 5, 0, 12, 'SRI-INSPECT-001', 5, 7, 32, 0),
(3, 15, 'NORMAL', NULL, 15, 'Completed normally', 15, 0, 15, 'SRI-OK-001', 5, 7, 33, 1),
(4, 8, 'NORMAL', NULL, 10, 'Expected 10, received 8', 8, 0, 10, 'SRI-SHORT-001', 5, 7, 34, 1),
(5, 12, 'NORMAL', NULL, 10, 'Expected 10, received 12', 12, 0, 10, 'SRI-SURPLUS-001', 5, 7, 35, 1);

-- ============================================================
-- SUPPLIER INVOICE
-- ============================================================

INSERT INTO supplier_invoices
(id, image_file_url, invoice_code, invoice_date, invoice_number, invoice_series, lookup_code, pdf_file_url, status, subtotal, supplier_tax_code, tax_amount, total_amount, updated_at, uploaded_at, verification_note, xml_file_url, purchase_order_id, supplier_id, uploaded_by, due_date)
VALUES
(1, NULL, 'INV-CODE-0001', CURDATE(), 'INV-2026-0001', 'AA/26E', 'LOOKUP-0001', NULL, 'VERIFIED', 12000000.00, 'TAX000001', 1200000.00, 13200000.00, NOW(), NOW(), 'Development verified invoice', NULL, 14, 1, 12, DATE_ADD(CURDATE(), INTERVAL 30 DAY));

-- ============================================================
-- SALES / REPLENISHMENT / ANALYTICS
-- ============================================================

INSERT INTO sales_orders
(id, created_at, customer_name, payment_method, sales_orders_code, status, total_amount, updated_at, created_by, store_id)
VALUES
(1, NOW(), 'Development Customer', 'CARD', 'SO-2026-0001', 'COMPLETED', 370000.00, NOW(), 11, 10),
(2, NOW(), 'Pending Customer', 'CASH', 'SO-2026-0002', 'DRAFT', 79000.00, NOW(), 11, 10);

INSERT INTO sales_order_items
(id, quantity, sales_order_items_code, subtotal, unit_price, product_id, sales_order_id)
VALUES
(1, 2, 'SOI-000001', 370000.00, 185000.00, 7, 1),
(2, 1, 'SOI-000002', 79000.00, 79000.00, 8, 2);

INSERT INTO replenishment_requests
(id, created_at, current_quantity, reason, replenishment_requests_code, request_code, requested_quantity, reviewed_at, status, updated_at, created_by, product_id, reviewed_by, store_id)
VALUES
(1, NOW(), 8, 'Inventory below target level', 'RPL-000001', 'REQ-2026-0001', 30, NULL, 'PENDING', NOW(), 11, 7, NULL, 10),
(2, DATE_SUB(NOW(), INTERVAL 1 DAY), 5, 'Store needs replenishment', 'RPL-000002', 'REQ-2026-0002', 20, NOW(), 'TRANSFER_CREATED', NOW(), 11, 8, 12, 10);

INSERT INTO analytics_results
(id, analysis_type, analytics_results_code, generated_at, result_data, score, product_id, store_id, warehouse_id)
VALUES
(1, 'DEMAND_FORECAST', 'AN-000001', NOW(), '{"forecast":42,"period":"7d"}', 0.9123, 7, 10, 9),
(2, 'STOCK_RISK', 'AN-000002', NOW(), '{"risk":"LOW"}', 0.1500, 8, 10, 9);

-- ============================================================
-- ACTIVITY / SECURITY
-- ============================================================

INSERT INTO activity_logs
(id, action, created_at, device_info, entity_id, entity_type, ip_address, new_value, old_value, user_id, activity_logs_code)
VALUES
(1, 'LOGIN', NOW(), 'Chrome Development', 13, 'AUTHENTICATION', '127.0.0.1', NULL, NULL, 13, 'LOG-000001'),
(2, 'CREATE', NOW(), 'Chrome Development', 31, 'STORE_RETURN', '127.0.0.1', '{"status":"SHIPPED"}', NULL, 11, 'LOG-000002');

INSERT INTO suspicious_activity_alerts
(id, alert_level, description, detected_at, resolution_note, resolved_at, reviewed_at, risk_score, status, suspicious_activity_alerts_code, updated_at, related_log_id, reviewed_by, user_id)
VALUES
(1, 'LOW', 'Development suspicious login alert', NOW(), NULL, NULL, NULL, 15.50, 'NEW', 'SAA-000001', NOW(), 1, NULL, 13);

SET FOREIGN_KEY_CHECKS = 1;
