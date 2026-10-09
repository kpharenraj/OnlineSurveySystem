# Online Survey System — Documentation

## 1. Project Overview

The **Online Survey System** is a web-based application that allows users to create, publish, distribute, and manage online surveys.

Survey creators can create questions, publish surveys through unique public links, collect responses, and analyze the collected data through dashboards and graphical representations.

Respondents can participate in surveys without necessarily creating an account.

---

## 2. Project Goal

The primary goal of the Online Survey System is to provide a centralized platform for:

- Creating surveys
- Managing survey questions
- Publishing surveys
- Sharing surveys
- Collecting responses
- Storing survey data
- Analyzing responses
- Visualizing survey results
- Managing users and surveys

---

## 3. Documentation

| Document | Description |
|---|---|
| [PRD.md](PRD.md) | Product Requirements Document |
| [REQUIREMENTS.md](REQUIREMENTS.md) | Functional and non-functional requirements |
| [ARCHITECTURE.md](ARCHITECTURE.md) | System architecture |
| [DATABASE.md](DATABASE.md) | Database design |
| [API.md](API.md) | REST API specification |
| [SECURITY.md](SECURITY.md) | Security architecture |
| [DEVELOPMENT.md](DEVELOPMENT.md) | Development guidelines |
| [TESTING.md](TESTING.md) | Testing strategy |
| [DEPLOYMENT.md](DEPLOYMENT.md) | Deployment documentation |
| [USER_ROLES.md](USER_ROLES.md) | User roles and permissions |
| [FEATURES.md](FEATURES.md) | Feature specifications |
| [UI_UX.md](UI_UX.md) | UI/UX specification |
| [PROJECT_ROADMAP.md](PROJECT_ROADMAP.md) | Development roadmap |

---

## 4. Technology Stack

### Frontend

- HTML5
- CSS3
- JavaScript
- Responsive custom UI

### Backend

- Java
- Spring Boot
- Spring MVC
- Spring JDBC
- Spring Security

### Database

- PostgreSQL (hosted on Supabase)

### Authentication

- JWT
- BCrypt

### Database Migration

- Flyway

### Build Tool

- Maven

### Testing

- JUnit
- Mockito
- Spring Boot Test
- Postman

### Version Control

- Git
- GitHub

---

## 5. Major Modules

The system consists of the following major modules:

1. Authentication
2. User Management
3. Survey Management
4. Question Management
5. Response Collection
6. Analytics
7. Dashboard
8. Administration

---

## 6. High-Level Architecture

```text
User
  |
  v
Web Browser
  |
  v
Frontend
  |
  | HTTP / REST
  v
Spring Boot Backend
  |
  +-- Controller Layer
  |
  +-- Service Layer
  |
  +-- Repository Layer
  |
  +-- Security Layer
  |
  v
Supabase PostgreSQL Database

```
---

## 7. Main User Types

### Survey Creator

Creates and manages surveys.

### Respondent

Participates in surveys.

### Administrator

Manages users and surveys.

---

## 8. Project Status

### Current stage:
Planning and Documentation

Implementation will begin after the requirements and development environment have been finalized.