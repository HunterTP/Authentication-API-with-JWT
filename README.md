# Authentication API with JWT

A secure, production-ready Java REST API for user authentication using JWT tokens and BCrypt password hashing with MySQL database.

> Enables secure authentication for other APIs using a shared secret key.

[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.oracle.com/java/technologies/downloads/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue.svg)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![MIT License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

## Features

- JWT-based authentication with customizable expiration
- BCrypt password hashing with pepper for additional security
- HTTPS support with custom SSL certificates
- MySQL database for persistent user storage
- RESTful API endpoints
- Complete Docker support for easy deployment
- Health check endpoint for monitoring

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

# 3. Configure environment variables
export DB_URL="jdbc:mysql://localhost:3306/authdb?useSSL=false&serverTimezone=UTC"
export DB_USER="your_db_user"
export DB_PASSWORD="your_db_password"
export JWT_SECRET="your_secret_key_at_least_32_chars"
export BCRYPT_PEPPER="your_pepper_string"
export KEYSTORE_PASS="your_keystore_password"
export KEY_PASS="your_key_password"

# 4. Build and run
mvn clean compile exec:java
```

## Configuration

All configuration is done via environment variables:

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `DB_URL` | MySQL JDBC connection URL | Yes | - |
| `DB_USER` | Database username | Yes | - |
| `DB_PASSWORD` | Database password | Yes | - |
| `JWT_SECRET` | Secret key for JWT signing (min 32 chars) | Yes | - |
| `JWT_EXPIRATION_MS` | Token expiration in milliseconds | No | 3600000 (1 hour) |
| `BCRYPT_PEPPER` | Additional pepper for password hashing | Yes | - |
| `BCRYPT_WORKLOAD` | BCrypt workload factor | No | 12 |
| `KEYSTORE_PASS` | SSL keystore password | Yes | - |
| `KEY_PASS` | SSL key password | Yes | - |

## API Endpoints

Both legacy (`/auth/...`) and versioned (`/v1/auth/...`) paths are supported.

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/v1/auth/register` | Register new user | No |
| POST | `/v1/auth/login` | Login and get token | No |
| DELETE | `/v1/auth/user/delete` | Delete user | Yes |
| PUT | `/v1/auth/user/password` | Update password | Yes |
| PUT | `/v1/auth/user/username` | Update username | Yes |
| GET | `/v1/api/health` | Health check | No |

> Legacy paths (`/auth/register`, `/auth/login`, etc.) also work for backward compatibility.

### Example Usage

All examples use the versioned path (`/v1/`). Legacy paths work identically.

#### Register a new user:
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
  "type": "Bearer",
  "expiresIn": 3600000
}
```

#### Authenticated Request:
```bash
curl -k -X DELETE https://localhost:8443/v1/auth/user/delete \
  -H "Authorization: Bearer <your_token>"
```

## Docker Architecture

### Services

| Container | Image | Port | Purpose |
|-----------|-------|------|---------|
| **mysql-db** | `mysql:8.0.33` | `3307:3306` | User database, auth plugin `mysql_native_password` |
| **java-api** | `Dockerfile` build | `8443:8443` | Java HTTPS server, JWT auth |

### How MySQL Initializes

On first start (empty volume), the MySQL container runs the official entrypoint which:

1. **`mysqld --initialize-insecure`** — creates the data directory with root having an **empty password**
2. **Temporary server** starts with `--skip-networking` (socket only)
3. **`docker_setup_db()`** — entrypoint connects via socket, sets root's password via `ALTER USER`, creates the database and default appuser
4. **Init scripts run** — files from `./init-db/` are executed in order:
   - **`init.sh`** drops the entrypoint-created appuser (which uses `caching_sha2_password`) and recreates it with `IDENTIFIED WITH mysql_native_password` — required for Java JDBC compatibility over Docker bridge
   - **`init.sql`** creates the `users` table and index
5. **MySQL restarts** normally — now root has a password and appuser uses `mysql_native_password`

### Healthchecks

Both containers have healthchecks so Docker Compose can manage startup order:

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
    test: ["CMD", "curl", "-f", "-k", "-s", "-o", "/dev/null", "https://localhost:8443/api/health"]
    interval: 5s
    start_period: 20s
    retries: 10
```

### Environment Variables

Copy `.env.example` to `.env` and configure:

```env
# Database
DB_ROOT_PASSWORD=rootpass
DB_NAME=authdb
DB_USER=appuser
DB_PASSWORD=appuserpass
DB_PORT=3306

# JWT
JWT_SECRET=mySuperSecureJWTSecretThatIsAtLeast32CharactersLong
JWT_EXPIRATION_MS=3600000

# BCrypt
BCRYPT_PEPPER=mySECRETPepperThatIsNotStoredInTheDB
BCRYPT_WORKLOAD=15

# SSL
KEYSTORE_PASS=123456
KEY_PASS=123456

# Optional: custom keystore path
# KEYSTORE_PATH=./custom-keystore.jks

# Server
API_PORT=8443
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

### Why mysql_native_password?

MySQL 8.0 defaults to `caching_sha2_password`, but the Java JDBC driver (`mysql-connector-java`) struggles with this plugin over Docker's bridge network. The error "Host not allowed" is misleading — the real issue is the auth handshake failing. The fix is `init.sh` which recreates the appuser with `IDENTIFIED WITH mysql_native_password`.

## SSL Certificate

The Docker build generates a **self-signed** keystore automatically. For production, use a real certificate:

### Using Let's Encrypt (free)

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

### Use it with Docker

```yaml
# docker-compose.yml — mount your keystore and set env vars:
services:
  java-api:
    volumes:
      - ./keystore.jks:/app/custom-keystore.jks
    environment:
      KEYSTORE_PATH: /app/custom-keystore.jks
      KEYSTORE_PASS: your_keystore_password
      KEY_PASS: your_key_password
```

The `KEYSTORE_PATH` env var is supported — if not set, it falls back to the built-in self-signed `keystore.jks`.

### Self-signed vs. real certificate

| | Self-signed (default) | Real certificate |
|---|---|---|
| Browser | &#x26A0; Security warning | &#x2705; Trusted |
| Setup | Automatic in Docker build | Manual (Let's Encrypt, CA, etc.) |
| Use case | Development / testing | Production |

## CI/CD Pipeline

Every push to `main` triggers [GitHub Actions](.github/workflows/docker.yml):

```
Checkout → cp .env → Buildx → docker build → docker compose up -d
                                              ↓
                                         Wait MySQL (60s)
                                              ↓
                                         Grant permissions (retry 10x)
                                              ↓
                                         Wait API (120s)
                                              ↓
                                         Check / Stop (down -v)
```

| Step | What it does |
|------|-------------|
| **Wait for MySQL** | Polls `docker inspect` for health status (up to 60s). Exit 1 if MySQL never becomes healthy. |
| **Grant permissions** | Runs `docker exec` to ensure appuser exists with `mysql_native_password`. Retries 10 times with 2s intervals. This is a safety net for volume-reuse scenarios. |
| **Wait for API** | Polls container health status (up to 120s). |
| **Stop containers** | `docker compose down -v` removes the MySQL volume, ensuring every CI run starts fresh. |

### Known Pitfall: MYSQL_PWD

Never set `MYSQL_PWD` in `docker-compose.yml`. The MySQL entrypoint's `docker_setup_db()` calls `mysql -u root` (without `-p`), intending to send an empty password (root starts with empty password from `--initialize-insecure`). If `MYSQL_PWD` is set, `libmysqlclient` reads it and sends that value instead → "Access denied" → ALTER USER fails → root stays empty → downstream problems.

## Project Structure

```
Authentication-API-with-JWT/
├── src/
│   ├── main/java/com/jwt/server/
│   │   ├── Main.java              # Application entry point
│   │   ├── handlers/              # HTTP request handlers
│   │   │   ├── RegisterHandler.java
│   │   │   ├── LoginHandler.java
│   │   │   ├── DeleteUserHandler.java
│   │   │   ├── UpdatePasswordHandler.java
│   │   │   ├── UpdateUsernameHandler.java
│   │   │   └── HealthHandler.java
│   │   └── utils/                 # Utility classes
│   │       ├── Config.java
│   │       ├── CorsUtils.java
│   │       ├── HttpsUtils.java
│   │       ├── JbcryptUtils.java
│   │       ├── JsonUtils.java
│   │       ├── JwtUtils.java
│   │       ├── RateLimiter.java
│   │       ├── ResponseUtils.java
│   │       ├── SqlUtils.java
│   │       └── ValidationUtils.java
│   ├── main/resources/
│   │   └── logback.xml
│   └── test/java/com/jwt/server/utils/
│       ├── ConfigTest.java
│       ├── JsonUtilsTest.java
│       ├── RateLimiterTest.java
│       └── ValidationUtilsTest.java
├── init-db/
│   ├── init.sh              # Creates appuser with mysql_native_password
│   └── init.sql             # Creates users table + index
├── target/                 # Build output (generated)
├── pom.xml                # Maven configuration
├── Dockerfile            # Container definition
├── docker-compose.yml     # Docker Compose configuration
├── .env.example          # Environment template
├── .github/
│   └── workflows/
│       └── docker.yml    # GitHub Actions CI pipeline
└── README.md
```

## Security Notes

- **Always change default passwords in production**
- Use a strong `JWT_SECRET` (minimum 32 characters recommended)
- Keep `BCRYPT_PEPPER` secret - never store it in the database
- Replace the default `keystore.jks` with a valid SSL certificate in production
- The BCrypt workload factor (12) provides good security without significant performance impact

## Tech Stack

- **Java 17** - Programming language
- **Maven** - Build tool
- **MySQL 8.0** - Database
- **jjwt** - JWT library
- **jBCrypt** - BCrypt password hashing
- **Docker** - Containerization

## License

MIT License - see [LICENSE](LICENSE) for details.

## Contributing

Contributions are welcome! Please open an issue or submit a pull request.