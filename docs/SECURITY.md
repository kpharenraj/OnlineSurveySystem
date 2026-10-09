# Security Design

## 1. Authentication

The application will use:

```text
Spring Security
+
JWT
```
Authentication flow:
```text
Login
  |
  v
Validate Credentials
  |
  v
Generate JWT
  |
  v
Return Token
  |
  v
Client Sends Bearer Token
  |
  v
JWT Filter
  |
  v
Validate Token
  |
  v
Protected Endpoint
```

## 2. Password Security

Passwords must never be stored as plain text.

Use:
```text
BCrypt
```

Example:
```text
Password
   ↓
BCrypt
   ↓
Password Hash
   ↓
Database
```

Password recovery uses a cryptographically random six-digit code. Only its
BCrypt hash is stored; codes expire after 10 minutes, are single-use, and allow
up to five verification attempts. Requests are limited to one per email per
minute, and the request endpoint does not reveal whether an email is registered.
SMTP credentials must be kept in environment variables and never committed.

---

## 3. Authorization

Roles:
```text
USER
ADMIN
```

A user must only be able to modify resources they own.

## 4. Ownership Validation

For example:
```text
Current User ID
       ==
Survey User ID
```
Only then should the operation be allowed.

---

## 5. Input Validation

Use Jakarta Bean Validation.

Examples:
```text
@NotBlank
@NotNull
@Email
@Size
@Min
@Max
```

---

## 6. SQL Injection Protection

Use:

- Spring JDBC with parameterized queries
- Parameterized queries
- Repository methods

Do not concatenate user input into SQL queries.

---

## 7. JWT Security

JWT secrets must not be stored directly in Git.

Use environment variables.

Example:
```text
JWT_SECRET=...
```

---

## 8. CORS

Only trusted frontend origins should be allowed.

Development:
```text
http://localhost:3000
```

or whichever frontend port is configured.

Production should use the real frontend domain.

---

## 9. Error Handling

Do not expose:

- Stack traces
- Database credentials
- SQL queries
- Passwords
- JWT tokens
- Internal implementation details

---

## 10. HTTPS

Production deployment must use HTTPS.

---

## 11. Future Security Improvements

Potential improvements:

- CAPTCHA
- Rate limiting
- Duplicate response detection
- Account lockout
- Email verification
- Audit logs
- Advanced moderation

---

## 12. Security Checklist
 - BCrypt password hashing
 - JWT authentication
 - Role authorization
 - Ownership validation
 - Input validation
 - SQL injection protection
 - Hashed, expiring, single-use password-reset codes
 - CORS configuration
 - Secret management
 - Safe error handling
 - HTTPS