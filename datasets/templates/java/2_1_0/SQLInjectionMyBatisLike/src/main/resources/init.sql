DROP TABLE IF EXISTS products;
CREATE TABLE products (
                          id INT PRIMARY KEY AUTO_INCREMENT,
                          name VARCHAR(255) NOT NULL,
                          description VARCHAR(255),
                          price DECIMAL(10, 2)
);

INSERT INTO products (name, description, price) VALUES ('Laptop', 'Powerful laptop for work', 1200.00);
INSERT INTO products (name, description, price) VALUES ('Mouse', 'Wireless ergonomic mouse', 25.50);
INSERT INTO products (name, description, price) VALUES ('Keyboard', 'Mechanical keyboard with RGB', 75.99);
