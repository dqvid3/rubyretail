CREATE DATABASE IF NOT EXISTS webmobile CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE webmobile;

CREATE TABLE categories (
    id           INT          NOT NULL AUTO_INCREMENT,
    name         VARCHAR(100) NOT NULL,
    picture_path VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE products (
    id               INT            NOT NULL AUTO_INCREMENT,
    name             VARCHAR(100)   NOT NULL,
    description      TEXT           NOT NULL,
    price            DECIMAL(10, 2) NOT NULL,
    width            DECIMAL(8, 2)  NOT NULL,
    height           DECIMAL(8, 2)  NOT NULL,
    length           DECIMAL(8, 2)  NOT NULL,
    main_picture_path VARCHAR(255)  NOT NULL,
    category_id      INT            NOT NULL,
    upload_date      TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE product_colors (
    id         INT         NOT NULL AUTO_INCREMENT,
    product_id INT         NOT NULL,
    color      VARCHAR(50) NOT NULL,
    color_hex  VARCHAR(7)  NOT NULL,
    stock      INT         NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE product_pictures (
    id            INT          NOT NULL AUTO_INCREMENT,
    product_id    INT          NOT NULL,
    picture_path  VARCHAR(255) NOT NULL,
    picture_index INT          NOT NULL,
    color_id      INT          NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_product_picture (product_id, picture_index, color_id),
    FOREIGN KEY (product_id) REFERENCES products (id),
    FOREIGN KEY (color_id) REFERENCES product_colors (id)
);

CREATE TABLE sales (
    id          INT      NOT NULL AUTO_INCREMENT,
    description TEXT     NOT NULL,
    discount    INT      NOT NULL,
    start_date  DATETIME NOT NULL,
    end_date    DATETIME NOT NULL,
    product_id  INT      NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE users (
    id                 INT          NOT NULL AUTO_INCREMENT,
    username           VARCHAR(50)  NOT NULL,
    name               VARCHAR(50)  NOT NULL,
    surname            VARCHAR(50)  NOT NULL,
    email              VARCHAR(100) NOT NULL,
    password           VARCHAR(255) NOT NULL,
    current_address_id INT          DEFAULT -1,
    current_card_id    INT          DEFAULT -1,
    PRIMARY KEY (id),
    UNIQUE KEY uq_username (username),
    UNIQUE KEY uq_email (email)
);

CREATE TABLE user_addresses (
    id            INT          NOT NULL AUTO_INCREMENT,
    user_id       INT          NOT NULL,
    name          VARCHAR(100) NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255) DEFAULT NULL,
    city          VARCHAR(100) NOT NULL,
    state         VARCHAR(100) NOT NULL,
    postal_code   VARCHAR(20)  NOT NULL,
    county        VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE user_payments (
    id              INT         NOT NULL AUTO_INCREMENT,
    user_id         INT         NOT NULL,
    card_number     VARCHAR(20) NOT NULL,
    cardholder_name VARCHAR(100) NOT NULL,
    cvv             INT         NOT NULL,
    expiration_date VARCHAR(7)  NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE orders (
    id          INT            NOT NULL AUTO_INCREMENT,
    user_id     INT            NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    order_date  TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    address_id  INT            NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id)    REFERENCES users (id),
    FOREIGN KEY (address_id) REFERENCES user_addresses (id)
);

CREATE TABLE order_items (
    id         INT            NOT NULL AUTO_INCREMENT,
    order_id   INT            NOT NULL,
    product_id INT            NOT NULL,
    color_id   INT            NOT NULL,
    quantity   INT            NOT NULL,
    price      DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (order_id)   REFERENCES orders (id),
    FOREIGN KEY (product_id) REFERENCES products (id),
    FOREIGN KEY (color_id)   REFERENCES product_colors (id)
);

CREATE TABLE cart_items (
    id         INT NOT NULL AUTO_INCREMENT,
    user_id    INT NOT NULL,
    product_id INT NOT NULL,
    color_id   INT NOT NULL,
    quantity   INT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id)    REFERENCES users (id),
    FOREIGN KEY (product_id) REFERENCES products (id),
    FOREIGN KEY (color_id)   REFERENCES product_colors (id)
);

CREATE TABLE wishlist_items (
    id         INT NOT NULL AUTO_INCREMENT,
    user_id    INT NOT NULL,
    product_id INT NOT NULL,
    color_id   INT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id)    REFERENCES users (id),
    FOREIGN KEY (product_id) REFERENCES products (id),
    FOREIGN KEY (color_id)   REFERENCES product_colors (id)
);

CREATE TABLE product_reviews (
    id          INT       NOT NULL AUTO_INCREMENT,
    user_id     INT       NOT NULL,
    product_id  INT       NOT NULL,
    rating      INT       NOT NULL,
    comment     TEXT,
    review_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id)    REFERENCES users (id),
    FOREIGN KEY (product_id) REFERENCES products (id)
);
