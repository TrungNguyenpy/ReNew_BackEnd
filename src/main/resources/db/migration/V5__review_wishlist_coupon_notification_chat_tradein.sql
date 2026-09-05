-- V5: Review, Wishlist, Coupon, Notification, Chat, Trade-in
-- Depends on V1 (users, categories), V2 (products), V4 (orders).

-- ============================================================
-- reviews
-- ============================================================
CREATE TABLE reviews (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id                  UUID NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    customer_id                 UUID NOT NULL REFERENCES users (id),
    order_id                    UUID NOT NULL REFERENCES orders (id),
    rating                      INTEGER NOT NULL,
    comment                     TEXT,
    product_condition_rating    INTEGER,
    delivery_rating             INTEGER,
    packaging_rating            INTEGER,
    seller_reply                TEXT,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_reviews_order_product UNIQUE (order_id, product_id),
    CONSTRAINT ck_reviews_rating_range CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT ck_reviews_condition_rating_range
        CHECK (product_condition_rating IS NULL OR product_condition_rating BETWEEN 1 AND 5),
    CONSTRAINT ck_reviews_delivery_rating_range
        CHECK (delivery_rating IS NULL OR delivery_rating BETWEEN 1 AND 5),
    CONSTRAINT ck_reviews_packaging_rating_range
        CHECK (packaging_rating IS NULL OR packaging_rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_reviews_product_id ON reviews (product_id);
CREATE INDEX idx_reviews_customer_id ON reviews (customer_id);

-- ============================================================
-- review_images
-- ============================================================
CREATE TABLE review_images (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id                UUID NOT NULL REFERENCES reviews (id) ON DELETE CASCADE,
    image_url                VARCHAR(500) NOT NULL,
    cloudinary_public_id     VARCHAR(255),
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_review_images_review_id ON review_images (review_id);

-- ============================================================
-- wishlists
-- ============================================================
CREATE TABLE wishlists (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_wishlists_user_id UNIQUE (user_id)
);

-- ============================================================
-- wishlist_items
-- ============================================================
CREATE TABLE wishlist_items (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wishlist_id           UUID NOT NULL REFERENCES wishlists (id) ON DELETE CASCADE,
    product_id            UUID NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    price_at_add_time     NUMERIC(12, 2) NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_wishlist_items_wishlist_product UNIQUE (wishlist_id, product_id)
);

CREATE INDEX idx_wishlist_items_product_id ON wishlist_items (product_id);

-- ============================================================
-- coupons
-- ============================================================
CREATE TABLE coupons (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                     VARCHAR(50) NOT NULL,
    description              VARCHAR(500),
    discount_type            VARCHAR(20) NOT NULL,
    discount_value           NUMERIC(12, 2),
    min_order_value          NUMERIC(12, 2),
    max_discount_amount      NUMERIC(12, 2),
    start_date               TIMESTAMPTZ NOT NULL,
    end_date                 TIMESTAMPTZ NOT NULL,
    max_usage                INTEGER,
    current_usage            INTEGER NOT NULL DEFAULT 0,
    per_user_limit           INTEGER,
    is_active                BOOLEAN NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_coupons_code UNIQUE (code),
    CONSTRAINT ck_coupons_discount_type
        CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT', 'FREE_SHIPPING')),
    CONSTRAINT ck_coupons_dates CHECK (end_date > start_date)
);

-- ============================================================
-- coupon_usages
-- ============================================================
CREATE TABLE coupon_usages (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    coupon_id            UUID NOT NULL REFERENCES coupons (id) ON DELETE CASCADE,
    user_id              UUID NOT NULL REFERENCES users (id),
    order_id             UUID NOT NULL REFERENCES orders (id),
    discount_applied     NUMERIC(12, 2) NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_coupon_usages_coupon_order UNIQUE (coupon_id, order_id)
);

CREATE INDEX idx_coupon_usages_coupon_user ON coupon_usages (coupon_id, user_id);

-- ============================================================
-- notifications
-- ============================================================
CREATE TABLE notifications (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type             VARCHAR(30) NOT NULL,
    title            VARCHAR(255) NOT NULL,
    message          TEXT NOT NULL,
    reference_type   VARCHAR(50),
    reference_id     UUID,
    is_read          BOOLEAN NOT NULL DEFAULT FALSE,
    read_at          TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_notifications_type CHECK (type IN (
        'ORDER_CREATED', 'ORDER_CONFIRMED', 'ORDER_SHIPPED', 'ORDER_DELIVERED',
        'ORDER_CANCELLED', 'PAYMENT_SUCCESS', 'PAYMENT_FAILED',
        'WARRANTY_EXPIRING', 'WISHLIST_PRICE_DROP'
    ))
);

CREATE INDEX idx_notifications_user_id ON notifications (user_id);
CREATE INDEX idx_notifications_user_unread ON notifications (user_id, is_read);

-- ============================================================
-- chat_rooms
-- ============================================================
CREATE TABLE chat_rooms (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    staff_id       UUID REFERENCES users (id),
    product_id     UUID REFERENCES products (id) ON DELETE SET NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_chat_rooms_status CHECK (status IN ('OPEN', 'CLOSED'))
);

CREATE INDEX idx_chat_rooms_customer_id ON chat_rooms (customer_id);
CREATE INDEX idx_chat_rooms_staff_id ON chat_rooms (staff_id);

-- ============================================================
-- chat_messages
-- ============================================================
CREATE TABLE chat_messages (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_room_id      UUID NOT NULL REFERENCES chat_rooms (id) ON DELETE CASCADE,
    sender_id         UUID NOT NULL REFERENCES users (id),
    content           TEXT NOT NULL,
    attachment_url    VARCHAR(500),
    is_read           BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_messages_chat_room_id ON chat_messages (chat_room_id);

-- ============================================================
-- trade_in_requests
-- ============================================================
CREATE TABLE trade_in_requests (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id          UUID NOT NULL REFERENCES users (id),
    product_name         VARCHAR(255) NOT NULL,
    brand                VARCHAR(150),
    model                VARCHAR(150),
    category_id          UUID REFERENCES categories (id),
    purchase_year        INTEGER,
    usage_duration       VARCHAR(100),
    condition            VARCHAR(20),
    description          TEXT,
    expected_price       NUMERIC(12, 2),
    offered_price        NUMERIC(12, 2),
    contact_phone        VARCHAR(20) NOT NULL,
    contact_email        VARCHAR(255),
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    inspected_by         UUID REFERENCES users (id),
    inspection_note      TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_trade_in_requests_condition
        CHECK (condition IS NULL OR condition IN ('EXCELLENT', 'VERY_GOOD', 'GOOD', 'FAIR', 'POOR')),
    CONSTRAINT ck_trade_in_requests_status CHECK (status IN (
        'PENDING', 'INSPECTING', 'OFFERED', 'CUSTOMER_ACCEPTED', 'PURCHASED', 'REJECTED'
    ))
);

CREATE INDEX idx_trade_in_requests_customer_id ON trade_in_requests (customer_id);
CREATE INDEX idx_trade_in_requests_status ON trade_in_requests (status);

-- ============================================================
-- trade_in_items
-- ============================================================
CREATE TABLE trade_in_items (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trade_in_request_id      UUID NOT NULL REFERENCES trade_in_requests (id) ON DELETE CASCADE,
    media_type               VARCHAR(10) NOT NULL,
    media_url                VARCHAR(500) NOT NULL,
    cloudinary_public_id     VARCHAR(255),
    display_order            INTEGER NOT NULL DEFAULT 0,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_trade_in_items_media_type CHECK (media_type IN ('IMAGE', 'VIDEO'))
);

CREATE INDEX idx_trade_in_items_request_id ON trade_in_items (trade_in_request_id);
