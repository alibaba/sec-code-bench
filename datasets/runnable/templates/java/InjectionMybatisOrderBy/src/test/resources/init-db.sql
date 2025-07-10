-- 创建用户表
DROP TABLE IF EXISTS users;

CREATE TABLE users (
                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                      name VARCHAR(255) NOT NULL,
                      age INT
);

-- 初始化测试数据
MERGE INTO users (id, name, age) KEY(id) VALUES(1, 'John', 18);