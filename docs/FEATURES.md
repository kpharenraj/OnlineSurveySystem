# Feature Specification

## 1. Authentication

Features:

- Registration
- Login
- Logout
- JWT authentication
- Password hashing
- Role-based authorization

---

## 2. User Management

Features:

- User profile
- Account information
- Role management
- Account administration

---

## 3. Survey Management

Features:

- Create survey
- View survey
- Edit survey
- Delete survey
- Save draft
- Publish survey
- Close survey
- Generate public link

---

## 4. Question Builder

Supported question types:

```text
TEXT
LONG_TEXT
SINGLE_CHOICE
MULTIPLE_CHOICE
RATING
YES_NO
```
Question settings:

- Required
- Optional
- Display order
- Options

---

## 5. Response Collection

Features:

- Public survey page
- Question rendering
- Client-side validation
- Server-side validation
- Response storage
- Submission confirmation

---

## 6. Analytics

Analytics include:

### Survey-level
- Total responses
- Total questions
- Average rating
- Response trends
### Question-level
- Answer count
- Percentage distribution
- Rating distribution
- Average rating

---

## 7. Dashboard

Dashboard cards:
```text
Total Surveys
Published Surveys
Total Responses
Average Rating
```

Dashboard sections:
```text
Recent Surveys
Response Trends
Question Analytics
Survey Performance
```

---

## 8. Administration

Admin features:

- User management
- Survey management
- Survey moderation
- Platform statistics

---

## 9. Future Features

Potential future features:

- CSV export
- Excel export
- PDF reports
- Email invitations
- Survey templates
- Scheduled publishing
- Conditional questions
- Multiple languages
- Advanced analytics
- AI-assisted survey creation

## Pages

### User authentication 

Register, log in, log out, and securely store passwords.

### Survey management

Create, edit, delete, publish, and close surveys.

### Question builder

Text, long text, single choice, multiple choice, rating, and yes/no questions.

### Public survey links

Share a link so respondents can complete a published survey without an account.

### Response collection

Validate answers and store submitted responses in the Supabase PostgreSQL database.

### Analytics and charts

View response counts, answer summaries, and graphical charts.

### Dashboard

Show surveys, publication status, and response statistics.

### Admin features

Manage users and moderate surveys.