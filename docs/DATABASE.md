```markdown
# Database Design

## 1. Database

Database:

```text
postgres (Supabase project default)
```
Database provider and engine:
```text
Supabase PostgreSQL
```

Create a Supabase project and use its PostgreSQL database for the application.
The project connection details are available in the Supabase dashboard.

---

## 2. Main Tables
```text
users
surveys
questions
question_options
responses
answers
```

---

## 3. Entity Relationship

```text
USER
 |
 | 1
 |
 | N
 v
SURVEY
 |
 | 1
 |
 | N
 v
QUESTION
 |
 | 1
 |
 | N
 v
QUESTION_OPTION


SURVEY
 |
 | 1
 |
 | N
 v
RESPONSE
 |
 | 1
 |
 | N
 v
ANSWER
 |
 | N
 |
 | 1
 v
QUESTION
```

---

## 4. Users Table
```text
users
----------------------------
id
name
email
password_hash
role
created_at
updated_at
```

Fields:
```text
Field	Type	Constraint
id	BIGINT	Primary Key
name	VARCHAR(100)	NOT NULL
email	VARCHAR(255)	UNIQUE
password_hash	VARCHAR(255)	NOT NULL
role	VARCHAR(30)	NOT NULL
created_at	TIMESTAMP	NOT NULL
updated_at	TIMESTAMP	NOT NULL
```
---

## 5. Surveys Table

```text
surveys
----------------------------
id
user_id
title
description
status
public_token
created_at
updated_at
```
Relationships:
```text
user_id → users.id
```

---

## 6. Questions Table
```text
questions
----------------------------
id
survey_id
question_text
question_type
question_order
required
```
Relationship:
```text
survey_id → surveys.id
```

---

## 7. Question Options Table

Used for:

- SINGLE_CHOICE
- MULTIPLE_CHOICE
- YES_NO

```text
question_options
----------------------------
id
question_id
option_text
option_order
```

---

## 8. Responses Table

```text
responses
----------------------------
id
survey_id
respondent_id
submitted_at
```
respondent_id may be NULL when anonymous responses are enabled.

---

## 9. Answers Table

```text
answers
----------------------------
id
response_id
question_id
answer_text
```

---

## 10. Relationships
```text
User
 |
 +----< Survey
          |
          +----< Question
          |        |
          |        +----< QuestionOption
          |
          +----< Response
                   |
                   +----< Answer
                            |
                            >---- Question
```

---

## 11. Recommended Indexes

```text
users.email

surveys.user_id
surveys.public_token
surveys.status

questions.survey_id

question_options.question_id

responses.survey_id

answers.response_id
answers.question_id
```

---

## 12. Database Rules
1. Email must be unique.
2. Public survey token must be unique.
3. Every survey must belong to a valid user.
4. Every question must belong to a valid survey.
5. Every response must belong to a valid survey.
6. Every answer must belong to a valid response.
7. Answers must reference valid questions.
8. Response submission should be transactional.

---

## 13. Database Migration

Flyway migrations will be stored in:
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
