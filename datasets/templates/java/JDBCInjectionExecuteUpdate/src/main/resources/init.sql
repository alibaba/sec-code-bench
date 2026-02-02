DROP TABLE IF EXISTS products;
CREATE TABLE products (
                          id INT PRIMARY KEY AUTO_INCREMENT,
                          name VARCHAR(255) NOT NULL,
                          description VARCHAR(255),
                          price DECIMAL(10, 2)
);

INSERT INTO products (name) VALUES ('Laptop');
INSERT INTO products (name) VALUES ('Mouse');
INSERT INTO products (name) VALUES ('Keyboard');