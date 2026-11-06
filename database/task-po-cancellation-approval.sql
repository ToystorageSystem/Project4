-- REQUIRED MIGRATION, run once after team/DBA approval.
-- MySQL 8+, database warehouse. Do NOT run automatically against shared MySQL.
-- Does not change purchase_orders status enum or existing PO records.

CREATE TABLE purchase_order_cancellation_requests (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    supplier_agreed BIT(1) NOT NULL DEFAULT b'0',
    requested_by BIGINT NOT NULL,
    requested_at DATETIME(6) NOT NULL,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    decision_note VARCHAR(1000) NULL,
    -- MySQL allows multiple NULL values in UNIQUE index.
    -- At most one PENDING request may exist per Purchase Order.
    pending_purchase_order_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN status = 'PENDING' THEN purchase_order_id ELSE NULL END) STORED,
    CONSTRAINT fk_po_cancel_order FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id),
    CONSTRAINT fk_po_cancel_requested_by FOREIGN KEY (requested_by) REFERENCES users(id),
    CONSTRAINT fk_po_cancel_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(id),
    CONSTRAINT uk_po_cancel_one_pending UNIQUE (pending_purchase_order_id),
    INDEX idx_po_cancel_order (purchase_order_id),
    INDEX idx_po_cancel_status (status, requested_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
