-- V2: Product catalog — products, product_images, product_specifications
-- Depends on V1 (categories, brands).

-- ============================================================
-- products
-- ============================================================
CREATE TABLE products (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                   VARCHAR(255) NOT NULL,
    slug                   VARCHAR(255) NOT NULL,
    description            TEXT,
    category_id            UUID NOT NULL REFERENCES categories (id),
    brand_id               UUID NOT NULL REFERENCES brands (id),
    model                  VARCHAR(150),
    price                  NUMERIC(12, 2) NOT NULL,
    original_price         NUMERIC(12, 2),
    stock_quantity         INTEGER NOT NULL DEFAULT 0,

    condition              VARCHAR(20) NOT NULL,
    manufacture_year       INTEGER,
    purchase_year          INTEGER,
    usage_duration         VARCHAR(100),
    cosmetic_condition     VARCHAR(500),
    functional_condition   VARCHAR(500),
    known_defects          TEXT,
    repair_history         TEXT,
    accessories_included   VARCHAR(500),

    is_active              BOOLEAN NOT NULL DEFAULT TRUE,
    is_hidden              BOOLEAN NOT NULL DEFAULT FALSE,
    version                BIGINT  NOT NULL DEFAULT 0,

    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_products_slug UNIQUE (slug),
    CONSTRAINT ck_products_condition
        CHECK (condition IN ('EXCELLENT', 'VERY_GOOD', 'GOOD', 'FAIR', 'POOR')),
    CONSTRAINT ck_products_stock_non_negative CHECK (stock_quantity >= 0),
    CONSTRAINT ck_products_price_non_negative CHECK (price >= 0)
);

CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_brand_id ON products (brand_id);
CREATE INDEX idx_products_condition ON products (condition);
CREATE INDEX idx_products_price ON products (price);
CREATE INDEX idx_products_active_hidden ON products (is_active, is_hidden);

-- ============================================================
-- product_images
-- ============================================================
CREATE TABLE product_images (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id             UUID NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    image_url              VARCHAR(500) NOT NULL,
    cloudinary_public_id   VARCHAR(255),
    image_type             VARCHAR(20) NOT NULL,
    display_order          INTEGER NOT NULL DEFAULT 0,
    is_primary             BOOLEAN NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_product_images_type CHECK (
        image_type IN ('FRONT', 'BACK', 'SIDE', 'SCREEN', 'SERIAL_LABEL', 'DEFECT', 'ACCESSORY')
    )
);

CREATE INDEX idx_product_images_product_id ON product_images (product_id);

-- ============================================================
-- product_specifications
-- ============================================================
CREATE TABLE product_specifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id      UUID NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    spec_key        VARCHAR(150) NOT NULL,
    spec_value      VARCHAR(500) NOT NULL,
    display_order   INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_product_specifications_product_id ON product_specifications (product_id);
