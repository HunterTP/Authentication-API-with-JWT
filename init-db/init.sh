#!/bin/bash
set -euo pipefail
# init-db/init.sh
# Runs during MySQL initialization (first startup with empty data volume).
# Ensures appuser uses mysql_native_password for Java JDBC compatibility.

mysql -u root -p"${MYSQL_ROOT_PASSWORD}" <<EOSQL
DROP USER IF EXISTS '${MYSQL_USER}'@'%';
CREATE USER '${MYSQL_USER}'@'%' IDENTIFIED WITH mysql_native_password BY '${MYSQL_PASSWORD}';
GRANT ALL PRIVILEGES ON \`${MYSQL_DATABASE}\`.* TO '${MYSQL_USER}'@'%';
FLUSH PRIVILEGES;
EOSQL
