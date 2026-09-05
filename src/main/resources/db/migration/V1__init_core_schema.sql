-- V1: Core schema — users, addresses, categories, brands
-- These are the foundational tables that later migrations (products, orders, etc.) will reference.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- users
-- ============================================================
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(150) NOT NULL,
    phone           VARCHAR(20),
    role            VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    avatar_url      VARCHAR(500),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('CUSTOMER', 'STAFF', 'ADMIN'))
);

CREATE INDEX idx_users_role ON users (role);

-- ============================================================
-- addresses
-- ============================================================
CREATE TABLE addresses (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recipient_name   VARCHAR(150) NOT NULL,
    recipient_phone  VARCHAR(20)  NOT NULL,
    address_line     VARCHAR(255) NOT NULL,
    ward             VARCHAR(100) NOT NULL,
    district         VARCHAR(100) NOT NULL,
    province         VARCHAR(100) NOT NULL,
    is_default       BOOLEAN      NOT NULL DEFAULT FALSE,
    note             VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_addresses_user_id ON addresses (user_id);

-- ============================================================
-- categories (self-referencing for hierarchy)
-- ============================================================
CREATE TABLE categories (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(150) NOT NULL,
    slug           VARCHAR(150) NOT NULL,
    description    VARCHAR(1000),
    image_url      VARCHAR(500),
    parent_id      UUID REFERENCES categories (id) ON DELETE SET NULL,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    display_order  INTEGER      NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_categories_slug UNIQUE (slug)
);

CREATE INDEX idx_categories_parent_id ON categories (parent_id);

-- ============================================================
-- brands
-- ============================================================
CREATE TABLE brands (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(150) NOT NULL,
    slug         VARCHAR(150) NOT NULL,
    logo_url     VARCHAR(500),
    description  VARCHAR(1000),
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_brands_name UNIQUE (name),
    CONSTRAINT uk_brands_slug UNIQUE (slug)
);
