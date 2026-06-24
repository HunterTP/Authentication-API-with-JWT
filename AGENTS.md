# Project Summary: Authentication-API-with-JWT

## Current Status ✅ (24.06.2026)
- **Java CI**: ✅ All checks passing
- **Docker Build**: ✅ All checks passing (fix confirmed working)
- **API endpoint**: ✅ Healthy (Docker + lokal)

## Root Cause: Docker Build CI Failure
The Java API container crashed during CI because MySQL's default `caching_sha2_password` authentication plugin is incompatible with `mysql-connector-java` (8.0.33) when connecting via Docker bridge network. The error:

```
Host '172.18.0.3' is not allowed to connect to this MySQL server
```

is actually a misleading error from the JDBC driver — the real issue is that `caching_sha2_password` requires RSA key exchange or SSL, and the Docker bridge network interferes with the public key retrieval. The error is reported as "Host not allowed" (MySQL error 1130) even though the actual problem is authentication plugin mismatch.

## Fixes Applied

### 1. `init-db/init.sh` — appuser with mysql_native_password
Runs during MySQL entrypoint init. Drops the entrypoint-created `appuser` (which uses `caching_sha2_password`) and recreates it with `IDENTIFIED WITH mysql_native_password` for JDBC compatibility. Uses `-p"${MYSQL_ROOT_PASSWORD}"` because it runs after the entrypoint sets root's password.

### 2. `docker-compose.yml` — Healthcheck with root password
MySQL healthcheck uses `mysqladmin ping -h localhost -u root -p"$MYSQL_ROOT_PASSWORD"` because the entrypoint now correctly sets root's password (no `MYSQL_PWD` interference).

**Key insight: `MYSQL_PWD` breaks the entrypoint**
- With `MYSQL_PWD=rootpass` set: the entrypoint's `mysql -u root` client reads `MYSQL_PWD` and sends `rootpass`, but root has an **empty password** (from `--initialize-insecure`) → "Access denied" → ALTER USER fails → root stays empty → healthcheck works without password
- Without `MYSQL_PWD`: entrypoint's `mysql -u root` sends **empty password** → matches root's empty password → ALTER USER succeeds → root has password → healthcheck/init.sh/grant-step all need `-p`

### 3. `.github/workflows/docker.yml` — CI polling statt --wait
- Replaced fixed `sleep 30` + `--wait` with proper polling loops:
  - **Wait for MySQL**: polls `docker inspect` for `healthy` (up to 60s)
  - **Wait for API**: same approach (up to 120s)
- Grant step uses `MYSQL_PWD="$MYSQL_ROOT_PASSWORD"` (set per-exec, not globally) to connect with root's password

### Removed: `mysql-config/custom.cnf`
- `default_authentication_plugin=mysql_native_password` is **deprecated** in MySQL 8.0.33 (produces WARNING)
- Unnecessary because `init.sh` creates appuser with explicit `IDENTIFIED WITH mysql_native_password`
- Removing it eliminates the deprecation warning and avoids any side effects on the entrypoint's ALTER USER

## Next Steps / Offene Punkte
- [ ] Git Actions secrets für `.env`-Variablen setzen (JWT_SECRET, KEYSTORE_PASS, etc.)
- [ ] Lokale Dummy-Werte aus `.env.example` entfernen vor Production-Deployment
