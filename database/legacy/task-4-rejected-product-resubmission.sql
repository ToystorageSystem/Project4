-- Task #4
-- Xu ly san pham bi tu choi va gui duyet lai.

-- Giu PENDING de tuong thich du lieu cu.
-- Them PENDING_CREATE va PENDING_UPDATE
-- phuc vu workflow moi.

ALTER TABLE products
    MODIFY COLUMN status ENUM(
        'PENDING',
        'PENDING_CREATE',
        'PENDING_UPDATE',
        'ACTIVE',
        'INACTIVE',
        'DISCONTINUED',
        'REJECTED'
    ) NOT NULL;
