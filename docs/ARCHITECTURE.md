# System Architecture

## 1. Architecture Style

The Online Survey System will use a **Layered Monolithic Architecture** using Spring Boot.

The main architecture is:

```text
Frontend
   |
   | HTTP / REST
   |
Controller Layer
   |
Service Layer
   |
Repository Layer
   |
Spring JDBC
   |
Supabase PostgreSQL
```

---

## 2. High-Level Architecture

```text
                    +----------------------+
                    |       USER           |
                    +----------+-----------+
                               |
                               v
                    +----------------------+
                    |    Web Browser       |
                    |   HTML/CSS/JS        |
                    +----------+-----------+
                               |
                           HTTP/REST
                               |
                               v
                    +----------------------+
                    |     Spring Boot      |
                    +----------+-----------+
                               |
             +-----------------+-----------------+
             |                 |                 |
             v                 v                 v
       Controller          Security           Exception
          Layer              Layer             Handler
             |
             v
        Service Layer
             |
             v
       Repository Layer
             |
             v
        Spring JDBC
             |
             v
       +-------------+
       |  Supabase   |
       | PostgreSQL |
       +-------------+
```

---

## 3. Controller Layer

The Controller layer handles HTTP requests.

### Responsibilities:

Receive requests
Validate request data
Call service methods
Return HTTP responses
Map DTOs

Controllers should not contain business logic.

Example:
```text
SurveyController
QuestionController
ResponseController
AnalyticsController
AuthController
AdminController
```

---

## 4. Service Layer

The Service layer contains business logic.

### Responsibilities:

Survey creation
Survey lifecycle
Ownership checks
Question management
Response validation
Analytics calculations
User management

Example:
```text
SurveyService
QuestionService
ResponseService
AnalyticsService
AuthService
UserService
```

---

## 5. Repository Layer

Application services communicate with PostgreSQL through parameterized Spring JDBC queries.

Example:
```text
UserRepository
SurveyRepository
QuestionRepository
QuestionOptionRepository
ResponseRepository
AnswerRepository
```

---

## 6. Entity Layer

Entities represent database tables.

Main entities:
```text
User
Survey
Question
QuestionOption
Response
Answer
```

---

## 7. DTO Layer

DTOs will be used for communication between frontend and backend.

Example:
```text
RegisterRequest
LoginRequest
SurveyRequest
QuestionRequest
ResponseRequest
SurveyResponse
AnalyticsResponse
```
DTOs prevent database entities from being exposed directly through APIs.

---

## 8. Security Layer

Spring Security will handle:

- Authentication
- JWT validation
- Authorization
- Role checks
- Password hashing

---

## 9. Suggested Package Structure

```text
src/
└── main/
    ├── java/
    │   └── com/
    │       └── surveysystem/
    │           ├── controller/
    │           ├── service/
    │           ├── repository/
    │           ├── entity/
    │           ├── dto/
    │           │   ├── request/
    │           │   └── response/
    │           ├── security/
    │           ├── exception/
    │           ├── mapper/
    │           └── config/
    │
    └── resources/
        ├── application.properties
        └── db/
            └── migration/
```

---

## 10. Survey Lifecycle
```text
        +-------+
        | DRAFT |
        +---+---+
            |
            | Publish
            v
      +-----------+
      | PUBLISHED |
      +-----+-----+
            |
            | Close
            v
       +---------+
       | CLOSED  |
       +---------+
```

---

## 11. Response Flow
```text
Respondent
    |
    v
Public Survey URL
    |
    v
Load Survey
    |
    v
Validate Survey Status
    |
    v
Display Questions
    |
    v
Respondent Answers
    |
    v
Validate Answers
    |
    v
Create Response
    |
    v
Create Answers
    |
    v
Database Transaction
    |
    v
Submission Successful
```

---

## 12. Analytics Flow
```text
Responses
    |
    v
Analytics Service
    |
    v
Aggregation
    |
    +----> Response Count
    |
    +----> Percentages
    |
    +----> Rating Average
    |
    +----> Question Statistics
    |
    v
Analytics API
    |
    v
Dashboard
    |
    v
Response Charts
```

---

## 13. Architecture Principles

The project will follow:

- Separation of concerns
- Single responsibility
- Dependency injection
- DTO-based APIs
- REST principles
- Centralized exception handling
- Server-side validation
- Transaction management
- Resource ownership validation