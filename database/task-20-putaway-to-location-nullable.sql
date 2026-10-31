-- Issue #20 - Receiving and Putaway integration
-- Warehouse Staff selects the destination shelf during Putaway.
-- This change was already applied to the shared MySQL database.
-- Keep this migration for other environments.

ALTER TABLE warehouse.putaway_task_items
MODIFY COLUMN to_location_id BIGINT NULL;
