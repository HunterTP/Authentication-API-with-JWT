# Project Summary: Authentication-API-with-JWT

## Current Status ✅
- **Java CI**: ✅ All checks passing
- **Docker Build**: ✅ All checks passing
- **API endpoint**: ✅ Healthy (Docker + lokal)

## Root Cause: Docker Build CI Failure
The Java API container crashed during CI because MySQL's default `caching_sha2_password` authentication plugin is incompatible with `mysql-connector-java` (8.0.33) when connecting via Docker bridge network. The error:

```
Host '172.18.0.3' is not allowed to connect to this MySQL server
```

is actually a misleading error from the JDBC driver — the real issue is that `caching_sha2_password` requires RSA key exchange or SSL, and the Docker bridge network interferes with the public key retrieval. The error is reported as "Host not allowed" (MySQL error 1130) even though the actual problem is authentication plugin mismatch.

## Fixes Applied

### 1. `mysql-config/custom.cnf` — MySQL auth plugin config
New config file that sets `default_authentication_plugin=mysql_native_password`. This forces MySQL 8.0.33 to use the older password hashing (natively supported by JDBC) instead of `caching_sha2_password`.

### 2. `init-db/init.sql` — Explicit user + mysql_native_password
Creates `'appuser'@'%'` explicitly with `IDENTIFIED WITH mysql_native_password`, plus ALTER USER as fallback if the user was already created with the wrong plugin.

### 3. `docker-compose.yml` — Healthcheck verbessert
- Switched from `wget` → `curl -f -k -s -o /dev/null` for quieter healthcheck
- Interval: 30s → 5s (faster failure detection)
- Added `start_period: 20s` (ignore failures during boot)
- Retries: 3 → 10
- Added `MYSQL_PWD` env var for root-level mysqladmin access

### 3. `Dockerfile` — curl installed in image
Added `apk add --no-cache curl` so the Docker healthcheck can use `curl` instead of `wget`.

### 4. `docker-compose.yml` — MySQL config mount
Added `./mysql-config/custom.cnf:/etc/mysql/conf.d/custom.cnf` volume mount to inject the `mysql_native_password` config into MySQL at startup.

### 5. `.github/workflows/docker.yml` — CI polling statt --wait
- Replaced fixed `sleep 30` + `--wait` with proper polling loops:
  - **Wait for MySQL**: polls `docker inspect` for `healthy` (up to 60s)
  - **Wait for API**: same approach (up to 120s)
- Removed diagnostic debug steps
- Build + start combined in one step

## Next Steps / Offene Punkte
- [ ] Git Actions secrets für `.env`-Variablen setzen (JWT_SECRET, KEYSTORE_PASS, etc.)
- [ ] Lokale Dummy-Werte aus `.env.example` entfernen vor Production-Deployment
