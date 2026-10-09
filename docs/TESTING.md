# Testing Strategy

## 1. Testing Levels

The project will use:

1. Unit Testing
2. Integration Testing
3. API Testing
4. UI Testing
5. Security Testing

---

## 2. Unit Testing

Tools:

```text
JUnit 5
Mockito
```

Primary targets:
```text
Services
Business logic
Validation
Analytics calculations
```

---

## 3. Integration Testing

Use:
```text
Spring Boot Test
```

Integration tests should verify:

- Controllers
- Services
- Repositories
- Database interactions

---

## 4. API Testing

Use:
```text
Postman
```
Collections:
```text
Online Survey System
|
+-- Authentication
+-- Surveys
+-- Questions
+-- Public Surveys
+-- Responses
+-- Analytics
+-- Admin
```

---

## 5. Authentication Test Cases
```text
| ID      | Test                             | Expected |
| ------- | -------------------------------- | -------- |
| AUTH-01 | Register valid user              | Success  |
| AUTH-02 | Register duplicate email         | Error    |
| AUTH-03 | Login valid credentials          | JWT      |
| AUTH-04 | Login invalid password           | 401      |
| AUTH-05 | Access protected API without JWT | 401      |
```

---

## 6. Survey Test Cases
```text
| ID     | Test                         | Expected |
| ------ | ---------------------------- | -------- |
| SUR-01 | Create survey                | Success  |
| SUR-02 | Update own survey            | Success  |
| SUR-03 | Update another user's survey | 403      |
| SUR-04 | Publish survey               | Success  |
| SUR-05 | Close survey                 | Success  |
| SUR-06 | Delete survey                | Success  |
```

---

## 7. Question Test Cases

```text
| ID     | Test                 | Expected         |
| ------ | -------------------- | ---------------- |
| QUE-01 | Add question         | Success          |
| QUE-02 | Edit question        | Success          |
| QUE-03 | Delete question      | Success          |
| QUE-04 | Add invalid question | Validation error |
| QUE-05 | Reorder questions    | Success          |
```

---

## 8. Response Test Cases
```text
| ID     | Test                    | Expected |
| ------ | ----------------------- | -------- |
| RES-01 | Submit valid response   | Success  |
| RES-02 | Missing required answer | Error    |
| RES-03 | Submit to closed survey | Error    |
| RES-04 | Invalid public token    | 404      |
| RES-05 | Invalid option          | Error    |
```

---

## 9. Analytics Test Cases
```text
| ID     | Test                          | Expected          |
| ------ | ----------------------------- | ----------------- |
| ANA-01 | Count responses               | Correct count     |
| ANA-02 | Calculate average rating      | Correct           |
| ANA-03 | Calculate choice distribution | Correct           |
| ANA-04 | Empty survey analytics        | Safe empty result |
```

---

## 10. Definition of Done

A feature is complete when:

- Code compiles
- Unit tests pass
- API tests pass
- Validation works
- Authorization works
- Error cases are handled
- Database operations work
- Documentation is updated