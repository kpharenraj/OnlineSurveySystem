# Development Guide

## 1. Prerequisites

Install:

- JDK 21 LTS
- Maven
- A Supabase project with its PostgreSQL database
- Git
- Postman
- VS Code / IntelliJ IDEA / Eclipse
- Modern web browser

---

## 2. Clone Repository

```bash
git clone <repository-url>
```

Then:
```bash
cd online-survey-system
```

---

## 3. Supabase Database Setup

Create a Supabase project, then get the PostgreSQL connection details from the
project dashboard. Use the JDBC-compatible connection URL and credentials
provided by Supabase. Require SSL for database connections.

---

## 4. Backend Configuration

Example configuration:
```properties
spring.datasource.url=${SUPABASE_DB_URL}
spring.datasource.username=${SUPABASE_DB_USERNAME}
spring.datasource.password=your_password

spring.flyway.enabled=true
```

Set `SUPABASE_DB_URL` to the JDBC PostgreSQL URL from Supabase (for example,
`jdbc:postgresql://<host>:5432/postgres?sslmode=require`). Store the username
and password in environment variables; do not commit credentials. Alternatively,
copy `.env.example` to `.env` in the repository root and set these values there.
Spring Boot loads `.env` automatically when the application starts.

The app uses a file-backed H2 database by default, so it can be run locally
without a Supabase account. To use Supabase, set the three database environment
variables above. Set `JWT_SECRET` to a private value of at least 32 bytes for
deployments. Never commit `.env` or share its credentials.

Email delivery is optional for local development. To enable welcome messages
and password recovery, set `SMTP_HOST=smtp.gmail.com`, `SMTP_PORT=587`,
`SMTP_USERNAME`, `SMTP_PASSWORD`, and `MAIL_FROM`. Use a Gmail App Password
generated after enabling 2-Step Verification, not your regular Google password.
Remove spaces if Google displays the App Password in groups.

---

## 5. Run Application

Build:
```bash
mvn clean install
```
Run:
```bash
mvn spring-boot:run
```
Application:
```text
http://localhost:8080
```

Start the app from the repository root with:
```bash
mvn spring-boot:run
```

Flyway creates the database tables on first launch. Register in the browser to
create an account and build a survey.

---

## 6. Maven Dependencies

Expected dependencies:
```text
Spring Web
Spring JDBC (JdbcTemplate)
Spring Security
Validation
PostgreSQL JDBC Driver
Flyway
JWT
Spring Boot Test
```

---

## 7. Coding Rules

Controllers should be thin.

Business logic belongs in services.

Database access belongs in repositories.

Use DTOs for APIs.

Validate all external input.

Avoid duplicated business logic.

Write tests for important functionality.

---

## 8. Git Branching

Recommended:
```text
main
 |
 +-- develop
      |
      +-- feature/authentication
      +-- feature/survey-management
      +-- feature/question-management
      +-- feature/responses
      +-- feature/analytics
```

---

## 9. Commit Convention

Examples:
```text
feat: add user registration
feat: add survey creation
fix: validate required questions
test: add survey service tests
docs: update database documentation
```

---

## 10. Flyway

Migration directory:
```text
src/main/resources/db/migration/
```
Example:

```text
V1__create_users.sql
V2__create_surveys.sql
V3__create_questions.sql
V4__create_question_options.sql
V5__create_responses.sql
V6__create_answers.sql
```

Never modify an already-applied migration.

Create a new migration instead.