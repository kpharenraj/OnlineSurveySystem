# System Requirements

## 1. Functional Requirements

### FR-01 User Registration

The system shall allow new users to create an account.

Required information:

- Name
- Email
- Password

---

### FR-02 User Login

The system shall allow registered users to authenticate using their credentials.

---

### FR-03 Authentication

The system shall authenticate protected API requests using JWT.

---

### FR-04 Authorization

The system shall restrict operations according to user roles and resource ownership.

---

### FR-05 Survey Creation

Authenticated users shall be able to create surveys.

A survey shall contain:

- Title
- Description
- Status
- Questions

---

### FR-06 Survey Editing

Survey creators shall be able to edit surveys that they own.

---

### FR-07 Survey Deletion

Survey creators shall be able to delete surveys that they own.

---

### FR-08 Survey Status

The system shall support:

```text
DRAFT
PUBLISHED
CLOSED
```

---

### FR-09 Question Management

Survey creators shall be able to:

- Add questions
- Edit questions
- Delete questions
- Reorder questions
- Mark questions as required

---

### FR-09 Question Management

Survey creators shall be able to:

- TEXT
- LONG_TEXT
- SINGLE_CHOICE
- MULTIPLE_CHOICE
- RATING
- YES_NO

---

### FR-11 Question Options

Choice-based questions shall support multiple options.

Example:

How satisfied are you?

○ Very Satisfied

○ Satisfied

○ Neutral

○ Dissatisfied

○ Very Dissatisfied

---

### FR-12 Survey Publishing

A creator shall be able to publish a completed survey.

Only published surveys shall be available to respondents.

---

### FR-13 Public Survey URL

Every published survey shall have a unique public token.

Example:
```text
/survey/8a72f91c
```

---

### FR-14 Response Submission

Respondents shall be able to submit answers to published surveys.

---

### FR-15 Response Validation

The system shall validate:

- Required questions
- Valid question IDs
- Valid choice options
- Rating ranges
- Survey status

---

### FR-16 Response Storage

The system shall store valid responses in the database.

---

### FR-17 Analytics

The system shall calculate:

- Total responses
- Response distribution
- Percentages
- Average ratings
- Question statistics

---

### FR-18 Dashboard

The creator dashboard shall display:

- Total surveys
- Published surveys
- Total responses
- Average rating
- Recent surveys
- Analytics

---

### FR-19 Administration

Administrators shall be able to:

- View users
- Manage users
- View surveys
- Moderate surveys
- View system statistics

---

## 2. Non-Functional Requirements

### NFR-01 Security

Passwords shall never be stored in plain text.

---

### NFR-02 Performance

The application should respond quickly under expected college-project workloads.

---

### NFR-03 Reliability

Successfully submitted responses should not be lost.

---

### NFR-04 Maintainability

The backend shall follow a layered architecture.

---

### NFR-05 Scalability

The system should allow additional functionality to be added later.

---

### NFR-06 Usability

The system should be understandable to users without technical knowledge.

---

### NFR-07 Compatibility

The application should work on modern browsers.

---

### NFR-08 Data Integrity

Database relationships and constraints shall prevent invalid data.

---

## 3. Hardware Requirements

Minimum:

- 4 GB RAM
- Dual-core processor
- 5 GB free storage

Recommended:

- 8 GB RAM or more
- Modern quad-core processor
- SSD storage

---

## 4. Software Requirements

Required:

- JDK 21 LTS
- Maven 3.9.16
- PostgreSQL database hosted on Supabase
- Git
- Postman
- Modern web browser
- IntelliJ IDEA / Eclipse / VS Code

---

## 5. Development Environment

### Backend:
```text
Java
Spring Boot
Maven
```
### Database:
```text
PostgreSQL (Supabase)
```
### Frontend:
```text
HTML
CSS
JavaScript
Responsive custom UI
```