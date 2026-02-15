# Estate Management Backend

Multi-tenant Estate Management and Security SaaS platform backend built with Spring Boot.

## Tech Stack

- **Java 21** + **Spring Boot 3.4.1**
- **PostgreSQL 16** (database)
- **Keycloak 24** (OAuth2/OIDC authentication)
- **Flyway** (database migrations)
- **MapStruct** (DTO mapping)
- **Maven** (multi-module build)

## Project Structure

```
estate-backend/
  estate-core/           # Entities, DTOs, enums, exceptions, mappers
  estate-infrastructure/ # JPA repositories, Flyway migrations, tenant filter
  estate-service/        # Business logic services, Keycloak admin client
  estate-api/            # REST controllers, security config, OpenAPI docs
  estate-app/            # Main application module, configuration
```

## Prerequisites

- Java 21
- Maven 3.9+
- Docker (for Keycloak)
- PostgreSQL 14+ (local or Docker)

## Setup

### 1. Database

Create the PostgreSQL database and user:

```sql
CREATE DATABASE estatekit;
CREATE USER estatekit WITH PASSWORD 'estatekit';
GRANT ALL PRIVILEGES ON DATABASE estatekit TO estatekit;
ALTER DATABASE estatekit OWNER TO estatekit;
```

### 2. Keycloak

Start Keycloak via Docker Compose:

```bash
docker compose up -d keycloak
```

Keycloak will be available at http://localhost:9090 (admin/admin).

The `estatekit` realm should be configured with:
- Client: `estate-backend` (confidential, service accounts enabled)
- Realm roles: `SUPER_ADMIN`, `ESTATE_ADMIN`, `FACILITY_MANAGER`, `RESIDENT`, `SECURITY_GUARD`
- Test user: `admin@estatekit.com` / `admin123` (SUPER_ADMIN role)

### 3. Build & Run

```bash
# Build all modules
mvn clean install

# Run the application
mvn -pl estate-app spring-boot:run
```

The application starts on **http://localhost:8083**.

### 4. Verify

```bash
# Health check
curl http://localhost:8083/actuator/health

# Get a token
TOKEN=$(curl -s -X POST "http://localhost:9090/realms/estatekit/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin@estatekit.com&password=admin123&grant_type=password&client_id=estate-backend&client_secret=<YOUR_CLIENT_SECRET>" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

# Create an estate
curl -X POST http://localhost:8083/api/v1/estates \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Estate","address":"123 Main St","estateType":"GATED_COMMUNITY"}'
```

## API Documentation

Swagger UI: http://localhost:8083/swagger-ui.html
OpenAPI JSON: http://localhost:8083/v3/api-docs

## API Endpoints

### Estates (`/api/v1/estates`)
| Method | Path | Description | Roles |
|--------|------|-------------|-------|
| POST | `/api/v1/estates` | Create estate | SUPER_ADMIN, ESTATE_ADMIN |
| GET | `/api/v1/estates/{id}` | Get estate by ID | Authenticated |
| GET | `/api/v1/estates` | List estates (paginated) | Authenticated |
| GET | `/api/v1/estates/search?query=` | Search estates | Authenticated |
| PUT | `/api/v1/estates/{id}` | Update estate | SUPER_ADMIN, ESTATE_ADMIN |
| DELETE | `/api/v1/estates/{id}` | Soft-delete estate | SUPER_ADMIN, ESTATE_ADMIN |

### Units (`/api/v1/units`)
| Method | Path | Description | Roles |
|--------|------|-------------|-------|
| POST | `/api/v1/units` | Create unit | SUPER_ADMIN, ESTATE_ADMIN |
| GET | `/api/v1/units/{id}` | Get unit by ID | Authenticated |
| GET | `/api/v1/units/estate/{estateId}` | List units by estate | Authenticated |
| PUT | `/api/v1/units/{id}` | Update unit | SUPER_ADMIN, ESTATE_ADMIN, FACILITY_MANAGER |
| DELETE | `/api/v1/units/{id}` | Soft-delete unit | SUPER_ADMIN, ESTATE_ADMIN |
| GET | `/api/v1/units/estate/{estateId}/count` | Count units | Authenticated |

## Running Tests

```bash
# Run all tests
mvn test

# Run specific module tests
mvn -pl estate-service test
```

## Configuration

Key configuration in `estate-app/src/main/resources/application.yml`:

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 8083 | Server port |
| `DB_HOST` | localhost | PostgreSQL host |
| `DB_PORT` | 5432 | PostgreSQL port |
| `DB_NAME` | estatekit | Database name |
| `KEYCLOAK_ISSUER_URI` | http://localhost:9090/realms/estatekit | Keycloak issuer |
