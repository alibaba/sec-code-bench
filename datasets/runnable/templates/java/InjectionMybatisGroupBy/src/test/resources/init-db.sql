-- 创建用户表
DROP TABLE IF EXISTS users;

CREATE TABLE users (
                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                      name VARCHAR(255) NOT NULL,
                      age INT
);
