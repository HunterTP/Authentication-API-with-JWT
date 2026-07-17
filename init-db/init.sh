#!/bin/bash
set -euo pipefail
# init-db/init.sh
# Runs during MySQL initialization (first startup with empty data volume).
# Ensures appuser uses mysql_native_password for Java JDBC compatibility
# and creates all required tables.

ROOT_OPTS="-u root -p${MYSQL_ROOT_PASSWORD}"

# Create appuser with mysql_native_password
mysql ${ROOT_OPTS} <<EOSQL
DROP USER IF EXISTS '${MYSQL_USER}'@'%';
CREATE USER '${MYSQL_USER}'@'%' IDENTIFIED WITH mysql_native_password BY '${MYSQL_PASSWORD}';
GRANT ALL PRIVILEGES ON \`${MYSQL_DATABASE}\`.* TO '${MYSQL_USER}'@'%';
FLUSH PRIVILEGES;
EOSQL

# Create all required tables
mysql ${ROOT_OPTS} "${MYSQL_DATABASE}" <<EOSQL
CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    salt VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username)
);
CREATE TABLE IF NOT EXISTS token_blacklist (
    token_hash VARCHAR(64) PRIMARY KEY,
    expires_at BIGINT NOT NULL,
    INDEX idx_token_expires_at (expires_at)
);
EOSQL
