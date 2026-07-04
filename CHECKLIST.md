# Production Readiness Checklist

## CI / Tests
- [ ] **Unit-Tests schreiben** — aktuell gibt es keine Tests (`src/test/` ist leer)
- [ ] **CI führt Tests aus** — `docker.yml` könnte `mvn test` vor dem Docker-Build laufen lassen
- [ ] **Integrationstests** — nach `docker compose up` die API-Endpunkte mit curl testen (register → login → delete)

## Sicherheit
- [ ] **Rate Limiting** — Brute-Force auf `/auth/login` verhindern (z.B. Token-Bucket oder IP-basiert)
- [ ] **Input-Validierung** — Username-Länge, Passwort-Mindestlänge, Sonderzeichen prüfen
- [ ] **Professionelles SSL** — Self-signed durch echtes Zertifikat ersetzen (Let's Encrypt etc.)
- [ ] **GitHub Secrets** — JWT_SECRET, BCRYPT_PEPPER, KEYSTORE_PASS in CI als Secrets setzen (nicht im .env.example)

## Code-Qualität
- [ ] **Logging-Framework** — `System.out.println` durch SLF4J + Logback ersetzen
- [ ] **Graceful Shutdown** — `Runtime.getRuntime().addShutdownHook()` für sauberes Herunterfahren
- [ ] **Konstanten auslagern** — Magische Werte (8443, 3600000) in Konstanten oder Config
- [ ] **API-Versioning** — `/v1/auth/register` statt `/auth/register`

## Docker
- [ ] **.dockerignore** — spart Build-Zeit (target/, .env, .git, node_modules, etc.)
- [ ] **Docker-Compose-Profile** — für dev vs. production (z.B. separate Ports, Logging)
- [ ] **Image-Tagging** — Docker-Image mit Version taggen statt nur `latest`

## Betrieb
- [ ] **Metrics-Endpoint** — z.B. Prometheus-kompatibel unter `/api/metrics`
- [ ] **Database-Migrations** — init.sql ist nur einmalig. Für Updates: z.B. Flyway oder Liquibase
- [ ] **Backup-Strategie** — MySQL-Volume regelmäßig sichern
- [ ] **Healthcheck verbessern** — `GET /api/health` sollte auch DB-Connectivity prüfen (aktuell tut es das nicht im Handler?)

## Nice-to-have
- [ ] **Swagger / OpenAPI** — API-Dokumentation automatisch generieren
- [ ] **Docker-Compose Watch** — Hot-Reload bei Code-Änderungen (Docker Compose v2.23+)
- [ ] **CORS** — falls API von Browser-Frontend aufgerufen wird
