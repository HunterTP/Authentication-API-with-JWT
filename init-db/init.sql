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

-- Fix: Allow appuser to connect from any host (Docker bridge network)
CREATE USER IF NOT EXISTS 'appuser'@'%' IDENTIFIED BY 'appuserpass';
GRANT ALL PRIVILEGES ON `authdb`.* TO 'appuser'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;
