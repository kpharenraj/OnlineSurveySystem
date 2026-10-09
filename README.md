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

### Email and password recovery

The app can run without email settings, but registration welcome emails and
password recovery require Gmail SMTP. In Google Account security settings,
enable 2-Step Verification and create an **App Password** for this app; use that
16-character App Password, not your normal Google password; remove any display
spaces if Google groups the characters.

Set `SMTP_HOST=smtp.gmail.com`, `SMTP_PORT=587`, `SMTP_USERNAME` to your Gmail
address, `SMTP_PASSWORD` to the App Password, and `MAIL_FROM` to that same Gmail
address in `.env`. Keep these secrets private. Password-reset codes expire after
10 minutes, are single-use, and can be requested at most once per minute.

### First administrator

For an optional admin account, set `ADMIN_EMAIL` and a private
`ADMIN_BOOTSTRAP_TOKEN` before starting the app. Register using that exact email
and enter the bootstrap token in the optional field. The first account for that
email becomes an administrator. Remove both bootstrap variables afterward;
the account keeps its admin role.

## Deploy on Render

The repository includes a Dockerfile and a Render Blueprint (`render.yaml`).
For a free demo deployment:

1. Push the application files to GitHub; `.env` is excluded by `.gitignore`
   and `.dockerignore`.
2. In the Render dashboard, choose **New → Blueprint**, connect this GitHub
   repository, and select the `main` branch. Review the service before applying
   the Blueprint.
3. Enter `SUPABASE_DB_URL`, `SUPABASE_DB_USERNAME`, `SUPABASE_DB_PASSWORD`,
   `JWT_SECRET`, `SMTP_USERNAME`, `SMTP_PASSWORD`, and `MAIL_FROM` in Render's
   environment settings. Set `SMTP_HOST` to `smtp.gmail.com` and `SMTP_PORT` to
   `587`. Use the Gmail App Password, not your normal Google password. For the
   session pooler, copy its host, port, and username from Supabase's **Connect**
   panel. The JDBC URL format is
   `jdbc:postgresql://<pooler-host>:<port>/postgres?sslmode=require`.
4. Deploy, then open the public `onrender.com` URL from the Render service page.

Render's free web services can sleep when idle, restart, and have an ephemeral
filesystem. This app therefore needs Supabase configured before deployment;
do not use its local H2 database for hosted data. The first start runs Flyway
to create the application tables in Supabase. Free services are suitable for
demos and testing, not production workloads.

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
