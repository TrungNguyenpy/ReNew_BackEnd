-- V4: Inventory, Cart, Order, Payment, Shipment
-- Depends on V1 (users), V2 (products).

-- ============================================================
-- inventories
-- ============================================================
CREATE TABLE inventories (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id       UUID NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    current_stock    INTEGER NOT NULL DEFAULT 0,
    available_stock  INTEGER NOT NULL DEFAULT 0,
    reserved_stock   INTEGER NOT NULL DEFAULT 0,
    sold_stock       INTEGER NOT NULL DEFAULT 0,
    damaged_stock    INTEGER NOT NULL DEFAULT 0,
    version          BIGINT  NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_inventories_product_id UNIQUE (product_id),
    CONSTRAINT ck_inventories_non_negative CHECK (
        current_stock >= 0 AND available_stock >= 0 AND reserved_stock >= 0
        AND sold_stock >= 0 AND damaged_stock >= 0
    )
);

-- ============================================================
-- inventory_histories
-- ============================================================
CREATE TABLE inventory_histories (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_id     UUID NOT NULL REFERENCES inventories (id) ON DELETE CASCADE,
    change_type      VARCHAR(20) NOT NULL,
    quantity_change  INTEGER NOT NULL,
    previous_stock   INTEGER NOT NULL,
    new_stock        INTEGER NOT NULL,
    note             VARCHAR(500),
    reference_type   VARCHAR(50),
    reference_id     UUID,
    changed_by       UUID REFERENCES users (id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_inventory_histories_type
        CHECK (change_type IN ('PURCHASE', 'SALE', 'RETURN', 'DAMAGE', 'ADJUSTMENT'))
);

CREATE INDEX idx_inventory_histories_inventory_id ON inventory_histories (inventory_id);

-- ============================================================
-- carts
-- ============================================================
CREATE TABLE carts (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_carts_user_id UNIQUE (user_id)
);

-- ============================================================
-- cart_items
-- ============================================================
CREATE TABLE cart_items (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id      UUID NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    product_id   UUID NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    quantity     INTEGER NOT NULL DEFAULT 1,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_cart_items_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT ck_cart_items_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_cart_items_cart_id ON cart_items (cart_id);

-- ============================================================
-- orders
-- ============================================================
CREATE TABLE orders (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number             VARCHAR(50) NOT NULL,
    customer_id              UUID NOT NULL REFERENCES users (id),
    status                   VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    recipient_name           VARCHAR(150) NOT NULL,
    recipient_phone          VARCHAR(20)  NOT NULL,
    shipping_address_line    VARCHAR(255) NOT NULL,
    shipping_ward            VARCHAR(100) NOT NULL,
    shipping_district        VARCHAR(100) NOT NULL,
    shipping_province        VARCHAR(100) NOT NULL,
    note                     VARCHAR(500),

    subtotal                 NUMERIC(12, 2) NOT NULL,
    shipping_fee             NUMERIC(12, 2) NOT NULL DEFAULT 0,
    discount_amount          NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_amount             NUMERIC(12, 2) NOT NULL,
    coupon_code              VARCHAR(50),

    payment_method           VARCHAR(20) NOT NULL,
    payment_status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    version                  BIGINT NOT NULL DEFAULT 0,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_orders_order_number UNIQUE (order_number),
    CONSTRAINT ck_orders_status CHECK (status IN (
        'PENDING', 'CONFIRMED', 'PROCESSING', 'PACKED', 'SHIPPED',
        'DELIVERED', 'CANCELLED', 'RETURN_REQUESTED', 'RETURNED', 'REFUNDED'
    )),
    CONSTRAINT ck_orders_payment_method CHECK (payment_method IN ('COD', 'STRIPE')),
    CONSTRAINT ck_orders_payment_status CHECK (payment_status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'REFUNDED'))
);

CREATE INDEX idx_orders_customer_id ON orders (customer_id);
CREATE INDEX idx_orders_status ON orders (status);

-- ============================================================
-- order_items
-- ============================================================
CREATE TABLE order_items (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id                 UUID NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id               UUID REFERENCES products (id) ON DELETE SET NULL,
    product_name_snapshot    VARCHAR(255) NOT NULL,
    condition_snapshot       VARCHAR(20),
    unit_price               NUMERIC(12, 2) NOT NULL,
    quantity                 INTEGER NOT NULL DEFAULT 1,
    subtotal                 NUMERIC(12, 2) NOT NULL,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_order_items_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);

-- ============================================================
-- order_status_histories
-- ============================================================
CREATE TABLE order_status_histories (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    status       VARCHAR(20) NOT NULL,
    note         VARCHAR(500),
    changed_by   UUID REFERENCES users (id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_order_status_histories_status CHECK (status IN (
        'PENDING', 'CONFIRMED', 'PROCESSING', 'PACKED', 'SHIPPED',
        'DELIVERED', 'CANCELLED', 'RETURN_REQUESTED', 'RETURNED', 'REFUNDED'
    ))
);

CREATE INDEX idx_order_status_histories_order_id ON order_status_histories (order_id);

-- ============================================================
-- payments
-- ============================================================
CREATE TABLE payments (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id                    UUID NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    method                      VARCHAR(20) NOT NULL,
    status                      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    amount                      NUMERIC(12, 2) NOT NULL,
    stripe_payment_intent_id    VARCHAR(255),
    stripe_charge_id            VARCHAR(255),
    paid_at                     TIMESTAMPTZ,
    failure_reason              VARCHAR(500),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_payments_order_id UNIQUE (order_id),
    CONSTRAINT ck_payments_method CHECK (method IN ('COD', 'STRIPE')),
    CONSTRAINT ck_payments_status CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'REFUNDED'))
);

CREATE INDEX idx_payments_stripe_payment_intent_id ON payments (stripe_payment_intent_id);

-- ============================================================
-- shipments
-- ============================================================
CREATE TABLE shipments (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id                    UUID NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    carrier                     VARCHAR(100),
    tracking_number             VARCHAR(100),
    estimated_delivery_date     DATE,
    shipped_at                  TIMESTAMPTZ,
    delivered_at                TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_shipments_order_id UNIQUE (order_id)
);
