# REST API Specification

## 1. Base URL

Local development:

```text
http://localhost:8080/api
```

---

## 2. Authentication APIs
### Register
```http
POST /api/auth/register
```
Request:
```JSON
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "Password123!"
}
```
Successful registration returns the authenticated session and an `emailSent`
boolean indicating whether the welcome email was delivered.
---
### Login
```http
POST /api/auth/login
```
Response:
```JSON
{
  "token": "JWT_TOKEN",
  "user": {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "role": "USER"
  }
}
```
---
### Request a password-reset code
```http
POST /api/auth/forgot-password
Content-Type: application/json
```
Request:
```json
{ "email": "you@example.com" }
```
The response is intentionally the same whether or not the address has an
account:
```json
{ "message": "If an account exists for that email, a reset code has been sent." }
```

### Reset a password
```http
POST /api/auth/reset-password
Content-Type: application/json
```
Request:
```json
{ "email": "you@example.com", "code": "123456", "password": "NewPassword123!" }
```
Codes expire after 10 minutes, can only be used once, and allow at most five
verification attempts. A new code can be requested once per minute.

---

## 3. Survey APIs
### Get Surveys
```http
GET /api/surveys
```
Get Survey
```http
GET /api/surveys/{id}
```

---

### Create Survey
```http
POST /api/surveys
```
Request:
```JSON
{
  "title": "Student Feedback",
  "description": "Feedback about the course"
}
```

---

### Update Survey
```http
PUT /api/surveys/{id}
```

---

### Delete Survey
```http
DELETE /api/surveys/{id}
```

---

### Publish Survey
```http
POST /api/surveys/{id}/publish
```

---

### Close Survey
```http
POST /api/surveys/{id}/close
```

---

## 4. Question APIs
### Add Question
```http
POST /api/surveys/{surveyId}/questions
```

Request:
```JSON
{
  "questionText": "How satisfied are you?",
  "questionType": "RATING",
  "required": true,
  "order": 1
}
```

---

### Update Question
```http
PUT /api/questions/{id}
```
---
### Delete Question
```http
DELETE /api/questions/{id}
```
---
### Reorder Questions
```http
PUT /api/surveys/{surveyId}/questions/order
```

---

## 5. Public Survey APIs
### Get Public Survey
```http
GET /api/public/surveys/{token}
```
### Submit Response
```http
POST /api/public/surveys/{token}/responses
```
Request:
```JSON
{
  "answers": [
    {
      "questionId": 1,
      "answer": "5"
    },
    {
      "questionId": 2,
      "answer": "Very useful"
    }
  ]
}
```

---

## 6. Response APIs
### Get Responses
```http
GET /api/surveys/{surveyId}/responses
```
### Get Response
```http
GET /api/responses/{id}
```

---

## 7. Analytics APIs
### Get Survey Analytics
```http
GET /api/surveys/{surveyId}/analytics
```

Example:
```JSON
{
  "totalResponses": 120,
  "averageRating": 4.3,
  "questions": []
}
```

---

## 8. Admin APIs
### Get Users
```http
GET /api/admin/users
```
### Get All Surveys
```http
GET /api/admin/surveys
```
### Delete User
```http
DELETE /api/admin/users/{id}
```
### Disable Survey
```http
POST /api/admin/surveys/{id}/disable
```

---

## 9. Authentication Header

Protected APIs require:
```http
Authorization: Bearer <JWT>
```

---

## 10. HTTP Status Codes
```text
Status	Meaning
200	OK
201	Created
204	No Content
400	Bad Request
401	Unauthorized
403	Forbidden
404	Not Found
409	Conflict
500	Internal Server Error
```

---
