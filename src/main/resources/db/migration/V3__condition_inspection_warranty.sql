-- V3: Condition scoring, inspection system, warranty
-- Depends on V2 (products). This migration ALTERs the already-applied
-- products table rather than editing V2, since V2 has already run against
-- the real database.

-- ============================================================
-- products: add denormalized overall condition score
-- ============================================================
ALTER TABLE products
    ADD COLUMN condition_score NUMERIC(5, 2);

ALTER TABLE products
    ADD CONSTRAINT ck_products_condition_score_range
        CHECK (condition_score IS NULL OR (condition_score >= 0 AND condition_score <= 100));

-- ============================================================
-- condition_score_items
-- ============================================================
CREATE TABLE condition_score_items (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id   UUID NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    criterion    VARCHAR(100) NOT NULL,
    score        INTEGER NOT NULL,
    note         VARCHAR(500),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_condition_score_items_range CHECK (score >= 0 AND score <= 100)
);

CREATE INDEX idx_condition_score_items_product_id ON condition_score_items (product_id);

-- ============================================================
-- product_inspections (header)
-- ============================================================
CREATE TABLE product_inspections (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id         UUID NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    inspector_id       UUID NOT NULL REFERENCES users (id),
    status             VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    inspection_date    TIMESTAMPTZ,
    inspection_score   INTEGER,
    result_summary     TEXT,
    internal_notes     TEXT,
    is_public          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_product_inspections_status
        CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED')),
    CONSTRAINT ck_product_inspections_score_range
        CHECK (inspection_score IS NULL OR (inspection_score >= 0 AND inspection_score <= 100))
);

CREATE INDEX idx_product_inspections_product_id ON product_inspections (product_id);
CREATE INDEX idx_product_inspections_inspector_id ON product_inspections (inspector_id);

-- ============================================================
-- inspection_items (checklist rows)
-- ============================================================
CREATE TABLE inspection_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inspection_id   UUID NOT NULL REFERENCES product_inspections (id) ON DELETE CASCADE,
    item_name       VARCHAR(100) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    note            VARCHAR(500),
    display_order   INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_inspection_items_status CHECK (status IN ('PASS', 'FAIL', 'WARNING'))
);

CREATE INDEX idx_inspection_items_inspection_id ON inspection_items (inspection_id);

-- ============================================================
-- warranties (one per product)
-- ============================================================
CREATE TABLE warranties (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id        UUID NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    warranty_type     VARCHAR(20) NOT NULL DEFAULT 'NONE',
    duration_months   INTEGER,
    start_date        DATE,
    end_date          DATE,
    policy            TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_warranties_product_id UNIQUE (product_id),
    CONSTRAINT ck_warranties_type CHECK (warranty_type IN ('MANUFACTURER', 'STORE', 'NONE'))
);
