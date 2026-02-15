# Running the Estate Management Application

## Quick Start (Local Development)

To run the application locally with an H2 embedded database, use the `dev` profile:

### Option 1: Using Maven
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### Option 2: Using Java (after building)
First, build the project:
```bash
mvn clean package
```

Then run:
```bash
java -jar estate-app/target/estate-app-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### Option 3: IDE Configuration (IntelliJ IDEA / JetBrains)
1. Edit your run configuration
2. Set `VM options` to: `-Dspring.profiles.active=dev`
3. Or set `Program arguments` to: `--spring.profiles.active=dev`
4. Run the application

## Available Profiles

### `dev` Profile (Default for Development)
- **Database**: H2 Embedded Database (in-memory)
- **Port**: 8083
- **Features**: 
  - H2 Console available at http://localhost:8083/h2-console
  - Database: `estatekit`
  - Username: `sa` (no password)
  - Full DDL auto schema creation (create-drop)

### `test` Profile (For Testing)
- **Database**: TestContainers PostgreSQL
- **Auto-creates PostgreSQL container for tests**

### `default` Profile (Production-like)
- **Database**: PostgreSQL (requires external PostgreSQL server)
- **Requires environment variables**:
  - `DB_HOST` (default: localhost)
  - `DB_PORT` (default: 5432)
  - `DB_NAME` (default: estatekit)
  - `DB_USERNAME` (default: estatekit)
  - `DB_PASSWORD` (default: estatekit)
  - `KEYCLOAK_SERVER_URL` (default: http://localhost:9090)
  - `KEYCLOAK_ISSUER_URI`
  - `KEYCLOAK_JWK_SET_URI`

## Database Migration

The application uses Flyway for database migrations. Place migration scripts in:
```
estate-app/src/main/resources/db/migration/
```

## Application URLs

When running locally with the `dev` profile:
- Main API: http://localhost:8083
- Swagger UI: http://localhost:8083/swagger-ui.html
- H2 Console: http://localhost:8083/h2-console
- Health Check: http://localhost:8083/actuator/health

## Troubleshooting

### Issue: "No active profile set, falling back to 1 default profile"
**Solution**: Make sure to activate the `dev` profile using the instructions above.

### Issue: "Failed to determine a suitable driver class"
**Solution**: The application is not finding the database driver. This typically means:
1. No profile is active (use the `dev` profile)
2. Or PostgreSQL is not running and environment variables are not set

### Issue: H2 Console not working
**Solution**: Ensure you're using the `dev` profile and try accessing http://localhost:8083/h2-console with:
- Driver: `org.h2.Driver`
- JDBC URL: `jdbc:h2:mem:estatekit`
- User: `sa`
- Password: (leave blank)

## Building Docker Image

To run with PostgreSQL in Docker:

```bash
docker-compose up
```

Then run the app with environment variables:
```bash
java -jar estate-app/target/estate-app-0.0.1-SNAPSHOT.jar \
  --DB_HOST=localhost \
  --DB_PORT=5432 \
  --DB_NAME=estatekit \
  --DB_USERNAME=estatekit \
  --DB_PASSWORD=estatekit
```

