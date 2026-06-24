-- init-db/init.sql

CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    salt VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Optional: Demo-User (only for Tests!)
INSERT INTO users (username, password, salt) 
SELECT * FROM (SELECT 'admin', '$2a$12$...', 'salt') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

-- Index for Performance
CREATE INDEX idx_username ON users(username);

-- Fix: Force mysql_native_password for Java JDBC compatibility
-- MySQL 8.0.33's default caching_sha2_password causes "Host not allowed" error
-- with Connector/J via Docker bridge network
CREATE USER IF NOT EXISTS 'appuser'@'%' IDENTIFIED WITH mysql_native_password BY 'appuserpass';
ALTER USER 'appuser'@'%' IDENTIFIED WITH mysql_native_password BY 'appuserpass';
GRANT ALL PRIVILEGES ON `authdb`.* TO 'appuser'@'%';
FLUSH PRIVILEGES;
