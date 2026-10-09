# Deployment Guide

## 1. Deployment Architecture

```text
Internet
   |
 HTTPS
   |
Reverse Proxy
   |
Spring Boot Application
   |
Supabase PostgreSQL
```

---

## 2. Build Application
```bash
mvn clean package
```

The JAR will normally be generated inside:
```text
target/
```

---

## 3. Run Application
```bash
java -jar target/online-survey-system.jar
```

---

## 4. Production Configuration

Production configuration should contain:

- Database URL
- Database username
- Database password
- JWT secret
- CORS configuration
- Server configuration

Secrets should be stored using environment variables or a secret-management system.

---

## 5. Supabase Database

Production should use a Supabase PostgreSQL project. Configure the application
with the JDBC connection URL and credentials from Supabase, require SSL, and
store credentials in environment variables or a secret-management system.

Database migrations should be handled through Flyway.

---

## 6. HTTPS

The production application should use HTTPS.

---

## 7. Logging

The application should log:

- Application startup
- Errors
- Security events
- Important application events

It must not log:

- Passwords
- JWT tokens
- Database passwords
- Sensitive respondent information

---

## 8. Database Backup

Production should have:

- Regular backups
- Backup retention
- Restore testing

---

## 9. Deployment Options

Possible deployment environments:

- Local server
- College server
- VPS
- Cloud VM
- Managed cloud services

---

## 10. Production Checklist
- [ ] Production database configured
- [ ] Secrets externalized
- [ ] HTTPS enabled
- [ ] CORS restricted
- [ ] Database backups configured
- [ ] Logging configured
- [ ] Flyway migrations verified
- [ ] Admin account secured
- [ ] Debug mode disabled