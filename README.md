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

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/auth/register` | Register new user | No |
| POST | `/auth/login` | Login and get token | No |
| DELETE | `/auth/user/delete` | Delete user | Yes |
| PUT | `/auth/user/password` | Update password | Yes |
| PUT | `/auth/user/username` | Update username | Yes |
| GET | `/api/health` | Health check | No |

### Example Usage

#### Register a new user:
```bash
curl -k -X POST https://localhost:8443/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "john", "password": "secret123"}'
```

#### Login:
```bash
curl -k -X POST https://localhost:8443/auth/login \
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
curl -k -X DELETE https://localhost:8443/auth/user/delete \
  -H "Authorization: Bearer <your_token>"
```

## Docker

### Compose File

The included `docker-compose.yml` starts two services:
- **MySQL 8.0** - Database container
- **Java API** - Application container

### Environment File (`.env`)

Copy `.env.example` to `.env` and configure:

```env
# Database
DB_ROOT_PASSWORD=your_root_password
DB_NAME=authdb
DB_USER=appuser
DB_PASSWORD=appuserpass
DB_PORT=3307

# JWT
JWT_SECRET=your_super_secret_jwt_key_at_least_32_characters

# BCrypt
BCRYPT_PEPPER=your_pepper_string
BCRYPT_WORKLOAD=12

# SSL
KEYSTORE_PASS=your_keystore_password
KEY_PASS=your_key_password

# Server
API_PORT=8443
```

### Build & Run

```bash
# Build and start all services
docker compose up -d

# View logs
docker compose logs -f java-api

# Stop services
docker compose down
```

## Project Structure

```
Authentication-API-with-JWT/
├── src/main/java/com/jwt/server/
│   ├── Main.java              # Application entry point
│   ├── handlers/            # HTTP request handlers
│   │   ├── RegisterHandler.java
│   │   ├── LoginHandler.java
│   │   ├── DeleteUserHandler.java
│   │   ├── UpdatePasswordHandler.java
│   │   ├── UpdateUsernameHandler.java
│   │   └── HealthHandler.java
│   └── utils/               # Utility classes
│       ├── SqlUtils.java
│       ├── JwtUtils.java
│       ├── JbcryptUtils.java
│       ├── HttpsUtils.java
│       └── ...
├── init-db/
│   └── init.sql             # Database initialization
├── target/                 # Build output (generated)
├── pom.xml                # Maven configuration
├── Dockerfile            # Container definition
├── docker-compose.yml     # Docker Compose configuration
├── .env.example        # Environment template
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