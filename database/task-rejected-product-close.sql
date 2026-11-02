-- Apply once, on a reviewed database backup / coordinated environment.
-- REJECTED remains the approval status; closed_at marks staff follow-up as complete.
ALTER TABLE product_change_requests
    ADD COLUMN closed_at DATETIME(6) NULL,
    ADD COLUMN closed_by BIGINT NULL,
    ADD KEY idx_product_change_requests_closed_at (closed_at),
    ADD CONSTRAINT fk_product_change_requests_closed_by
        FOREIGN KEY (closed_by) REFERENCES users (id);
