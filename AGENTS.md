# Project Summary: Authentication-API-with-JWT

## Current Status ✅
- **Java CI**: ✅ All checks passing
- **Docker Build**: ✅ All checks passing
- **API endpoint**: ✅ Healthy (Docker + lokal)

## Root Cause: Docker Build CI Failure
The Java API container crashed during CI because MySQL rejected the `appuser` connection from the Docker bridge network IP (`172.18.0.3`). The error was:

```
Host '172.18.0.3' is not allowed to connect to this MySQL server
```

MySQL's `MYSQL_USER`/`MYSQL_PASSWORD` env vars did not properly create `'appuser'@'%'`, so the Java API could never connect → restart loop → healthcheck never green.

## Fixes Applied

### 1. `init-db/init.sql` — Explicit MySQL user + grants
Added `CREATE USER IF NOT EXISTS 'appuser'@'%'` and `GRANT ALL PRIVILEGES ON authdb.* TO 'appuser'@'%'` to guarantee the app user can connect from any Docker IP regardless of what the MySQL entrypoint does.

### 2. `docker-compose.yml` — Healthcheck verbessert
- Switched from `wget` → `curl -f -k -s -o /dev/null` for quieter healthcheck
- Interval: 30s → 5s (faster failure detection)
- Added `start_period: 20s` (ignore failures during boot)
- Retries: 3 → 10
- Added `MYSQL_PWD` env var for root-level mysqladmin access

### 3. `Dockerfile` — curl installed in image
Added `apk add --no-cache curl` so the Docker healthcheck can use `curl` instead of `wget`.

### 4. `.github/workflows/docker.yml` — CI polling statt --wait
- Replaced fixed `sleep 30` + `--wait` with proper polling loops:
  - **Wait for MySQL**: polls `docker inspect` for `healthy` (up to 60s)
  - **Wait for API**: same approach (up to 120s)
- Removed diagnostic debug steps
- Build + start combined in one step

## Next Steps / Offene Punkte
- [ ] Git Actions secrets für `.env`-Variablen setzen (JWT_SECRET, KEYSTORE_PASS, etc.)
- [ ] Lokale Dummy-Werte aus `.env.example` entfernen vor Production-Deployment
