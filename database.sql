CREATE DATABASE IF NOT EXISTS jpa_web
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE jpa_web;

CREATE TABLE IF NOT EXISTS categories (
    category_id INT NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(255) NOT NULL,
    images VARCHAR(500) NULL,
    status INT NOT NULL DEFAULT 1,
    PRIMARY KEY (category_id),
    CONSTRAINT uk_categories_category_name UNIQUE (category_name),
    CONSTRAINT chk_categories_status CHECK (status IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS videos (
    video_id VARCHAR(100) NOT NULL,
    active INT NOT NULL DEFAULT 1,
    description TEXT NULL,
    poster VARCHAR(500) NULL,
    title VARCHAR(255) NULL,
    views INT NOT NULL DEFAULT 0,
    category_id INT NULL,
    PRIMARY KEY (video_id),
    INDEX idx_videos_category_id (category_id),
    CONSTRAINT fk_videos_categories
        FOREIGN KEY (category_id)
        REFERENCES categories (category_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_videos_active CHECK (active IN (0, 1)),
    CONSTRAINT chk_videos_views CHECK (views >= 0)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS users (
    user_id INT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_active CHECK (active IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS otp_tokens (
    otp_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    code_hash VARCHAR(100) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    attempts_remaining INT NOT NULL DEFAULT 5,
    last_sent_at DATETIME(6) NOT NULL,
    verified_at DATETIME(6) NULL,
    used_at DATETIME(6) NULL,
    PRIMARY KEY (otp_id),
    INDEX idx_otp_tokens_user_purpose (user_id, purpose, otp_id),
    CONSTRAINT fk_otp_tokens_users
        FOREIGN KEY (user_id)
        REFERENCES users (user_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT chk_otp_tokens_purpose CHECK (purpose IN ('REGISTER', 'RESET_PASSWORD')),
    CONSTRAINT chk_otp_tokens_attempts CHECK (attempts_remaining >= 0)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS products (
    product_id INT NOT NULL AUTO_INCREMENT,
    product_name VARCHAR(255) NOT NULL,
    price DECIMAL(15, 2) NOT NULL,
    description TEXT NULL,
    images VARCHAR(500) NULL,
    create_date DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    status INT NOT NULL DEFAULT 1,
    category_id INT NOT NULL,
    PRIMARY KEY (product_id),
    INDEX idx_products_create_date (create_date),
    INDEX idx_products_category_id (category_id),
    CONSTRAINT fk_products_categories
        FOREIGN KEY (category_id)
        REFERENCES categories (category_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_products_price CHECK (price >= 0),
    CONSTRAINT chk_products_status CHECK (status IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT IGNORE INTO categories (category_name, images, status) VALUES
    ('Smartphones', 'default.png', 1),
    ('Laptops', 'default.png', 0),
    ('Tablets', 'default.png', 1),
    ('Smartwatches', 'default.png', 0),
    ('Headphones', 'default.png', 1),
    ('Cameras', 'default.png', 0),
    ('Televisions', 'default.png', 1),
    ('Speakers', 'default.png', 0),
    ('Accessories', 'default.png', 1),
    ('Gaming', 'default.png', 0);

INSERT INTO products (product_name, price, description, images, create_date, status, category_id)
SELECT 'Nova X1 Smartphone', 699.99, 'A balanced smartphone with a bright display and all-day battery life.',
       'default.png', CURRENT_TIMESTAMP(6), 1, c.category_id
FROM categories c
WHERE c.category_name = 'Smartphones'
  AND NOT EXISTS (SELECT 1 FROM products p WHERE p.product_name = 'Nova X1 Smartphone');

INSERT INTO products (product_name, price, description, images, create_date, status, category_id)
SELECT 'Apex Pro Laptop', 1299.00, 'A lightweight performance laptop for study, work, and creative projects.',
       'default.png', CURRENT_TIMESTAMP(6) - INTERVAL 1 MINUTE, 1, c.category_id
FROM categories c
WHERE c.category_name = 'Laptops'
  AND NOT EXISTS (SELECT 1 FROM products p WHERE p.product_name = 'Apex Pro Laptop');

INSERT INTO products (product_name, price, description, images, create_date, status, category_id)
SELECT 'Canvas 11 Tablet', 499.00, 'An eleven-inch tablet designed for entertainment, notes, and everyday tasks.',
       'default.png', CURRENT_TIMESTAMP(6) - INTERVAL 2 MINUTE, 1, c.category_id
FROM categories c
WHERE c.category_name = 'Tablets'
  AND NOT EXISTS (SELECT 1 FROM products p WHERE p.product_name = 'Canvas 11 Tablet');

INSERT INTO products (product_name, price, description, images, create_date, status, category_id)
SELECT 'Pulse Fit Smartwatch', 249.00, 'A fitness smartwatch with health tracking and customizable watch faces.',
       'default.png', CURRENT_TIMESTAMP(6) - INTERVAL 3 MINUTE, 1, c.category_id
FROM categories c
WHERE c.category_name = 'Smartwatches'
  AND NOT EXISTS (SELECT 1 FROM products p WHERE p.product_name = 'Pulse Fit Smartwatch');

INSERT INTO products (product_name, price, description, images, create_date, status, category_id)
SELECT 'Echo Max Headphones', 179.00, 'Wireless over-ear headphones with clear sound and active noise reduction.',
       'default.png', CURRENT_TIMESTAMP(6) - INTERVAL 4 MINUTE, 1, c.category_id
FROM categories c
WHERE c.category_name = 'Headphones'
  AND NOT EXISTS (SELECT 1 FROM products p WHERE p.product_name = 'Echo Max Headphones');

INSERT INTO products (product_name, price, description, images, create_date, status, category_id)
SELECT 'Focus Mirrorless Camera', 899.00, 'A compact mirrorless camera for sharp photos and high-quality video.',
       'default.png', CURRENT_TIMESTAMP(6) - INTERVAL 5 MINUTE, 1, c.category_id
FROM categories c
WHERE c.category_name = 'Cameras'
  AND NOT EXISTS (SELECT 1 FROM products p WHERE p.product_name = 'Focus Mirrorless Camera');

INSERT INTO products (product_name, price, description, images, create_date, status, category_id)
SELECT 'Vision 55 Television', 749.00, 'A fifty-five-inch 4K smart television with vivid color and streaming apps.',
       'default.png', CURRENT_TIMESTAMP(6) - INTERVAL 6 MINUTE, 1, c.category_id
FROM categories c
WHERE c.category_name = 'Televisions'
  AND NOT EXISTS (SELECT 1 FROM products p WHERE p.product_name = 'Vision 55 Television');

INSERT INTO products (product_name, price, description, images, create_date, status, category_id)
SELECT 'SoundBox Speaker', 129.00, 'A portable wireless speaker with rich sound and a durable design.',
       'default.png', CURRENT_TIMESTAMP(6) - INTERVAL 7 MINUTE, 1, c.category_id
FROM categories c
WHERE c.category_name = 'Speakers'
  AND NOT EXISTS (SELECT 1 FROM products p WHERE p.product_name = 'SoundBox Speaker');

INSERT INTO products (product_name, price, description, images, create_date, status, category_id)
SELECT 'ChargeHub USB-C Dock', 89.00, 'A compact USB-C dock with charging, display, network, and data ports.',
       'default.png', CURRENT_TIMESTAMP(6) - INTERVAL 8 MINUTE, 1, c.category_id
FROM categories c
WHERE c.category_name = 'Accessories'
  AND NOT EXISTS (SELECT 1 FROM products p WHERE p.product_name = 'ChargeHub USB-C Dock');

INSERT INTO products (product_name, price, description, images, create_date, status, category_id)
SELECT 'Titan Gaming Console', 499.00, 'A modern gaming console built for fast loading and smooth gameplay.',
       'default.png', CURRENT_TIMESTAMP(6) - INTERVAL 9 MINUTE, 1, c.category_id
FROM categories c
WHERE c.category_name = 'Gaming'
  AND NOT EXISTS (SELECT 1 FROM products p WHERE p.product_name = 'Titan Gaming Console');
