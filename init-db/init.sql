-- init-db/init.sql
-- Note: User creation with mysql_native_password is handled by init.sh

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
