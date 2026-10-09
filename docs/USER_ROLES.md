# User Roles and Permissions

## 1. Roles

The system contains:

```text
USER
ADMIN
RESPONDENT
```

A respondent can participate without creating an account.

---

## 2. USER

A normal user can:

- Register
- Login
- Create surveys
- Edit owned surveys
- Delete owned surveys
- Add questions
- Edit questions
- Delete questions
- Publish surveys
- Close surveys
- View responses
- View analytics

A normal user cannot:

- Modify another user's survey
- Access another user's private analytics
- Manage users
- Perform administrator operations

---

## 3. ADMIN

An administrator can:

- View users
- Manage users
- View surveys
- Moderate surveys
- View platform statistics

---

## 4. RESPONDENT

A respondent can:

- Open published surveys
- View survey questions
- Submit answers
- Receive confirmation

A respondent cannot:

- Modify surveys
- Access creator dashboards
- Access private analytics
- Manage users

---

## 5. Permission Matrix

```text
| Action              | USER | ADMIN | RESPONDENT |
| ------------------- | ---: | ----: | ---------: |
| Register            |  Yes |   Yes |         No |
| Login               |  Yes |   Yes |         No |
| Create survey       |  Yes |   Yes |         No |
| Edit own survey     |  Yes |   Yes |         No |
| Edit another survey |   No |   Yes |         No |
| Publish survey      |  Yes |   Yes |         No |
| Submit response     | Yes* |  Yes* |        Yes |
| View own analytics  |  Yes |   Yes |         No |
| Manage users        |   No |   Yes |         No |
| Moderate surveys    |   No |   Yes |         No |
```
* Subject to the application's survey participation rules.
---

## 6. Ownership Rule

Before modifying a survey:
```text
Current User ID
       ==
Survey Owner ID
```

If the values do not match:
```text
403 FORBIDDEN
```
---
