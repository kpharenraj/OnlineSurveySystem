# Gather — Online Survey System

Gather is a full-stack survey application built with Java 21, Spring Boot, a
browser-based UI, and PostgreSQL on Supabase. Create and publish surveys, share
public links, collect anonymous answers, and review response analytics.

## Run locally

Requirements: JDK 21 and Maven 3.9 or newer.

```powershell
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080). On the first run, the app
creates a local file-backed H2 database in `data/`, so no Supabase account or
database setup is needed to try it. Register an account to create your first
survey.

To use Supabase instead:

1. Create a Supabase project and open its **Connect** panel to get the JDBC
   connection details.
2. Copy `.env.example` to `.env` in the repository root.
3. Fill in `SUPABASE_DB_URL`, `SUPABASE_DB_USERNAME`, and `SUPABASE_DB_PASSWORD`.
   The JDBC URL must start with `jdbc:postgresql:` and require SSL
   (for example, end it with `?sslmode=require`).
4. Set `JWT_SECRET` to a private random value of at least 32 bytes.
5. Run `mvn spring-boot:run`. Flyway initializes the tables in your Supabase
   database automatically.

Spring Boot reads the root `.env` file when the app starts. The file is ignored
by Git; never commit or share real database credentials or a JWT secret.

### First administrator

For an optional admin account, set `ADMIN_EMAIL` and a private
`ADMIN_BOOTSTRAP_TOKEN` before starting the app. Register using that exact email
and enter the bootstrap token in the optional field. The first account for that
email becomes an administrator. Remove both bootstrap variables afterward;
the account keeps its admin role.

## Included features

- Password-hashed registration, sign-in, and JWT-authenticated APIs
- Owned survey drafts, editing, deletion, publishing, and closing
- Required questions, ordering, and text, long-text, choice, yes/no, and rating types
- Public, anonymous survey response pages with server-side validation
- Per-question response distributions, rating averages, and individual answers
- Optional admin tools for account and survey moderation

See [docs/](docs/README.md) for the product, API, security, testing, and deployment
specifications. API calls are documented in [docs/API.md](docs/API.md).

## Build and test

```powershell
mvn test
mvn package
```
