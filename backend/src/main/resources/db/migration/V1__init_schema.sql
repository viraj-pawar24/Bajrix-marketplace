-- BajriX marketplace schema (MySQL 8+)
-- Product = catalogue entry. Seller = registered merchant. SellerListing =
-- one seller's price/stock/MOQ offer against one product (the many-to-many
-- join, with its own attributes).

CREATE TABLE sellers (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    contact_email  VARCHAR(255) NOT NULL UNIQUE,
    status         VARCHAR(20)  NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_seller_status CHECK (status IN ('APPROVED', 'PENDING', 'REJECTED'))
) ENGINE=InnoDB;

CREATE TABLE products (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    brand        VARCHAR(255),
    category     VARCHAR(120) NOT NULL,
    unit         VARCHAR(120),
    description  VARCHAR(2000),
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE INDEX idx_products_name ON products (name);
CREATE INDEX idx_products_category ON products (category);
-- keyword search over demo-scale data; see README for the real-scale plan
-- (MySQL FULLTEXT index, or an external search engine).
CREATE INDEX idx_products_brand ON products (brand);

CREATE TABLE seller_listings (
    id             BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id     BIGINT         NOT NULL,
    seller_id      BIGINT         NOT NULL,
    price          DECIMAL(12, 2) NOT NULL,
    stock          INTEGER        NOT NULL,
    min_order_qty  INTEGER        NOT NULL,
    active         BOOLEAN        NOT NULL DEFAULT TRUE,
    version        BIGINT         NOT NULL DEFAULT 0,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_listing_product_seller UNIQUE (product_id, seller_id),
    CONSTRAINT fk_listing_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_listing_seller FOREIGN KEY (seller_id) REFERENCES sellers(id) ON DELETE CASCADE,
    CONSTRAINT chk_listing_price CHECK (price > 0),
    CONSTRAINT chk_listing_stock CHECK (stock >= 0),
    CONSTRAINT chk_listing_moq CHECK (min_order_qty >= 1)
) ENGINE=InnoDB;

CREATE INDEX idx_listing_product ON seller_listings (product_id);
CREATE INDEX idx_listing_seller ON seller_listings (seller_id);
-- Speeds up "cheapest visible listing per product" lookups used by the
-- catalogue's "from ₹x" price and the product detail page.
CREATE INDEX idx_listing_visible_price ON seller_listings (product_id, active, price);
