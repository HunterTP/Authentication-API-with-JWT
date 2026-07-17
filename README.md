# Authentication API with JWT

A secure, production-ready Java REST API for user authentication using JWT tokens and BCrypt password hashing with MySQL database.

[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.oracle.com/java/technologies/downloads/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue.svg)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![MIT License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

## Features

- JWT-based authentication (HS256) with customizable expiration
- BCrypt password hashing with pepper for additional security
- HTTPS support with auto-generated self-signed SSL certificates
- MySQL database for persistent user storage
- RESTful API endpoints with versioned path (`/v1/auth/*`)
- Rate limiting: 20 requests per 60 seconds per IP (sliding window)
- Account jailing: 5 failed logins → 15-minute lockout per user
- Token blacklist: invalidates tokens on password/username change or account deletion
- Security headers: HSTS, CSP, X-Content-Type-Options, X-Frame-Options
- Content-Type validation: rejects non-JSON request bodies with 415
- Input validation: username regex, password length + whitespace checks
- JSON-injection safe error responses
- Complete Docker support for easy deployment
- Health check endpoint for monitoring
- Thread pool executor for concurrent request handling
- Token blacklist persisted in database (survives restarts)
- CI/CD pipeline with unit tests, integration tests, OWASP dependency check, and SpotBugs
- VS Code launch config with automatic `.env` loading
- 24-step sequential test suite (`requests.http`) importable by REST Clients

## Quick Start

### Using Docker (Recommended)

```bash
# 1. Clone the repository
git clone https://github.com/HunterTP/Authentication-API-with-JWT.git
cd Authentication-API-with-JWT

# 2. Configure environment
cp .env.example .env
# Edit .env with your settings

# 3. Start containers
docker compose up -d

# 4. Check status
docker compose ps
```

The API will be available at `https://localhost:8443`

### Local Development

```bash
# 1. Clone and setup
git clone https://github.com/HunterTP/Authentication-API-with-JWT.git
cd Authentication-API-with-JWT

# 2. Create MySQL database
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS authdb;"
mysql -u root -p authdb < init-db/init.sql

# 3. Configure environment
cp .env.example .env
# Edit .env with your settings (JWT_SECRET must be at least 32 chars)

# 4. Open in VS Code — launch config reads .env automatically
code .
# Run → "Run Authentication Server" (or F5)

# Or via Maven (first run downloads ~30 MB of dependencies)
mvn compile exec:java
```

> **Note**: `JWT_SECRET` is **required**. The server fails fast at first login attempt if it's not set. The VS Code launch config and `mvn exec:java` both load it automatically from the environment.

## Configuration

All configuration is via environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_URL` | MySQL JDBC URL | `jdbc:mysql://localhost:3306/authdb` |
| `DB_USER` | Database username | `root` |
| `DB_PASSWORD` | Database password | `root` |
| `JWT_SECRET` | JWT signing key (min 32 chars) | **Required** — fails fast if missing |
| `JWT_EXPIRATION_MS` | Token TTL in milliseconds | `3600000` (1 hour) |
| `BCRYPT_PEPPER` | Pepper prepended to passwords | `null` (no pepper) |
| `BCRYPT_WORKLOAD` | BCrypt cost factor | `12` |
| `KEYSTORE_PASS` | Keystore password | **Required** — fails fast if missing |
| `KEY_PASS` | Key password | **Required** — fails fast if missing |
| `KEYSTORE_PATH` | Custom keystore path | Built-in `keystore.jks` |
| `CORS_ORIGIN` | Allowed CORS origin | `*` |
| `API_PORT` | HTTPS server port | `8443` |

## API Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/v1/auth/register` | Register new user | No |
| POST | `/v1/auth/login` | Login and get token | No |
| DELETE | `/v1/auth/user/delete` | Delete user | Yes |
| PUT | `/v1/auth/user/password` | Update password | Yes |
| PUT | `/v1/auth/user/username` | Update username | Yes |
| GET | `/v1/api/health` | Health check | No |

All mutation endpoints (`register`, `login`, `password`, `username`) require `Content-Type: application/json`. Requests with missing or wrong Content-Type receive `415 Unsupported Media Type`.

### Example Usage

#### Register:
```bash
curl -k -X POST https://localhost:8443/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "john", "password": "secret123"}'
```

#### Login:
```bash
curl -k -X POST https://localhost:8443/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "john", "password": "secret123"}'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "message": "Login successful"
}
```

#### Authenticated Request:
```bash
curl -k -X DELETE https://localhost:8443/v1/auth/user/delete \
  -H "Authorization: Bearer <your_token>"
```

The full sequential 24-request test suite is in [`requests.http`](requests.http) (importable by VS Code REST Client or IntelliJ HTTP Client).

## Docker Architecture

### Services

| Container | Image | Port | Purpose |
|-----------|-------|------|---------|
| **mysql-db** | `mysql:8.0.33` | `3307:3306` | User database, auth plugin `mysql_native_password` |
| **java-api** | `Dockerfile` build | `8443:8443` | Java HTTPS server, JWT auth |

### How MySQL Initializes

On first start (empty volume), the MySQL container runs the official entrypoint which:

1. **`mysqld --initialize-insecure`** — creates the data directory with root having an empty password
2. **Temporary server** starts with `--skip-networking` (socket only)
3. **`docker_setup_db()`** — entrypoint connects via socket, sets root's password via `ALTER USER`, creates the database and default appuser
4. **Init scripts run** — files from `./init-db/` are executed in order:
   - **`init.sh`** drops the entrypoint-created appuser (which uses `caching_sha2_password`) and recreates it with `IDENTIFIED WITH mysql_native_password` — required for Java JDBC compatibility over Docker bridge
   - **`init.sql`** creates the `users` table and index
5. **MySQL restarts** normally — root now has a password and appuser uses `mysql_native_password`

### Healthchecks

```yaml
mysql-db:
  healthcheck:
    test: mysqladmin ping -h localhost -u root -p"$MYSQL_ROOT_PASSWORD"
    interval: 10s
    start_period: 10s
    retries: 5

java-api:
  depends_on:
    mysql-db:
      condition: service_healthy
  healthcheck:
    test: ["CMD", "curl", "-f", "-k", "-s", "-o", "/dev/null", "https://localhost:8443/v1/api/health"]
    interval: 5s
    start_period: 20s
    retries: 10
```

### Commands

```bash
# Build and start all services
docker compose up -d

# View logs
docker compose logs -f java-api

# Stop and remove volumes (fresh start)
docker compose down -v

# Execute MySQL commands
docker exec -it authenticationapi-mysql mysql -u root -p

# Rebuild the API image
docker compose build --no-cache java-api
```

## SSL Certificate

The Docker build generates a **self-signed** keystore automatically. For production, use a real certificate:

```bash
# 1. Get certificate (e.g. with certbot)
sudo certbot certonly --standalone -d yourdomain.com

# 2. Convert to PKCS12
openssl pkcs12 -export \
  -in /etc/letsencrypt/live/yourdomain.com/fullchain.pem \
  -inkey /etc/letsencrypt/live/yourdomain.com/privkey.pem \
  -out keystore.p12 \
  -name tomcat \
  -password pass:your_keystore_password

# 3. Convert PKCS12 to JKS
keytool -importkeystore \
  -srckeystore keystore.p12 -srcstoretype PKCS12 -srcstorepass your_keystore_password \
  -destkeystore keystore.jks -deststoretype JKS -deststorepass your_keystore_password
```

Set `KEYSTORE_PATH` in your environment or `docker-compose.yml` to use a custom keystore.

## Security

### Security Headers

Every response includes:

| Header | Value |
|--------|-------|
| `Strict-Transport-Security` | `max-age=63072000; includeSubDomains` |
| `Content-Security-Policy` | `default-src 'none'` |
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` |

### Rate Limiting

Per-IP sliding window: up to **20 requests per 60 seconds**. Exceeding returns `429 Too Many Requests`.

### Account Jailing

After **5 failed login attempts** for the same username, the account is locked for **15 minutes**. Further attempts return `429 Account is temporarily locked`. Successful login resets the counter.

### Token Blacklist

When a user changes their password or username (or deletes their account), the current JWT is immediately invalidated. The client must re-authenticate to get a new token.

### Input Validation

| Field | Rules |
|-------|-------|
| Username | 3-30 chars, `[a-zA-Z0-9_-]` only |
| Password | 8-128 chars, no leading/trailing whitespace |

### Error Handling

- No SQL error messages leak to the client (generic `500 Internal Server Error`)
- All error messages are JSON-escaped to prevent injection
- `HttpException` carries status code + message, caught centrally by `Middleware`
- Invalid methods return `405 Method Not Allowed`
- Wrong Content-Type returns `415 Unsupported Media Type`
- Missing/invalid tokens return `401 Unauthorized`

## CI/CD Pipeline

Every push to `main` triggers [GitHub Actions](.github/workflows/docker.yml):

```
Checkout → mvn test → OWASP DepCheck → SpotBugs → Buildx → docker build → docker compose up -d
                                                                                ↓
                                                                          Wait MySQL (60s)
                                                                                ↓
                                                                          Grant permissions (retry 10x)
                                                                                ↓
                                                                          Wait API (120s)
                                                                                ↓
                                                                          Integration tests
                                                                                ↓
                                                                          Stop (down -v)
```

| Step | Purpose |
|------|---------|
| `mvn test` | 25 unit tests (utilities: config, JSON, rate limiter, validation) |
| **OWASP Dependency Check** | Vulnerability scan of all dependencies (CVSS ≥ 7 fails build, non-blocking) |
| **SpotBugs** | Static analysis for bug patterns (Medium threshold, non-blocking) |
| **Integration tests** | Full register → login → delete cycle against running containers |

## Project Structure

```
Authentication-API-with-JWT/
├── src/
│   ├── main/java/com/jwt/server/
│   │   ├── Main.java                  # Entry point
│   │   ├── config/
│   │   │   └── Config.java            # Environment config
│   │   ├── handler/
│   │   │   ├── LoginHandler.java
│   │   │   ├── RegisterHandler.java
│   │   │   ├── DeleteUserHandler.java
│   │   │   ├── UpdatePasswordHandler.java
│   │   │   ├── UpdateUsernameHandler.java
│   │   │   └── HealthHandler.java
│   │   ├── middleware/
│   │   │   └── Middleware.java         # CORS, method validation, Content-Type,
│   │   │                               # token extraction, exception safety
│   │   ├── security/
│   │   │   ├── JwtUtils.java          # JWT generation + validation
│   │   │   ├── CorsUtils.java         # CORS headers
│   │   │   ├── RateLimiter.java       # Per-IP sliding window rate limiter
│   │   │   └── AccountLocker.java     # Per-account brute-force protection
│   │   ├── persistence/
│   │   │   ├── SqlUtils.java          # Database operations
│   │   │   ├── TokenBlacklist.java    # Post-mutation token invalidation
│   │   │   └── JbcryptUtils.java      # BCrypt + pepper hashing
│   │   ├── http/
│   │   │   ├── HttpsUtils.java        # SSL server setup
│   │   │   ├── RequestUtils.java      # Content-Type validation
│   │   │   ├── ResponseUtils.java     # JSON responses + security headers
│   │   │   └── JsonUtils.java         # Simple JSON parser
│   │   ├── exception/
│   │   │   └── HttpException.java     # Status code + message exception
│   │   └── validation/
│   │       └── ValidationUtils.java   # Username/password validation
│   ├── main/resources/
│   │   └── logback.xml
│   └── test/java/com/jwt/server/
│       ├── config/ConfigTest.java
│       ├── http/JsonUtilsTest.java
│       ├── security/RateLimiterTest.java
│       └── validation/ValidationUtilsTest.java
├── init-db/
│   ├── init.sh              # Creates appuser with mysql_native_password
│   └── init.sql             # Creates users table + index
├── .env.example             # Environment variable template
├── .env                     # Local environment (git-ignored)
├── .vscode/
│   ├── launch.json          # VS Code launch config (loads .env)
│   └── settings.json
├── requests.http            # 24-step sequential test suite (VS Code / IntelliJ)
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── .github/workflows/
│   └── docker.yml           # GitHub Actions CI
└── README.md
```

## Tech Stack

- **Java 17** — Language
- **Maven** — Build tool
- **MySQL 8.0** — Database
- **jjwt** — JWT library
- **jBCrypt** — BCrypt password hashing
- **Logback** — Structured logging (SLF4J)
- **JUnit 5** — Testing
- **Docker / Docker Compose** — Containerization
- **GitHub Actions** — CI/CD

## License

MIT License — see [LICENSE](LICENSE) for details.
