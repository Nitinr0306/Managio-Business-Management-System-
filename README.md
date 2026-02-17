# Managio — Gym & Fitness Studio Management SaaS

> **Version 1.0.0** · Enterprise-grade multi-tenant SaaS backend for managing gyms, fitness studios, and similar membership-based businesses.

![Java](https://img.shields.io/badge/Java-21+-ED8B00?style=flat-square&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![License](https://img.shields.io/badge/License-Proprietary-red?style=flat-square)
![Version](https://img.shields.io/badge/Version-1.0.0-blue?style=flat-square)

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Configuration](#configuration)
  - [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Module Breakdown](#module-breakdown)
- [Security](#security)
- [Scheduled Jobs](#scheduled-jobs)
- [Project Structure](#project-structure)
- [Environment Variables](#environment-variables)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

**Managio** is a production-ready, multi-tenant SaaS backend that allows gym owners and fitness studio operators to manage their entire business from one platform. It supports multiple businesses per owner, granular staff permissions, member self-service portals, subscription lifecycle management, payment tracking, and comprehensive audit logging.

---

## Features

### 👤 Authentication & Authorization
- JWT-based stateless authentication (access + refresh tokens)
- Role-based access control (RBAC) with `USER`, `ADMIN`, `SUPER_ADMIN` system roles
- OAuth2 social login support
- Two-factor authentication (2FA) ready
- Account lockout after configurable failed login attempts
- Token rotation and revocation
- Rate limiting on sensitive auth endpoints

### 🏢 Multi-Business Management
- One user can own and manage multiple businesses
- Full business lifecycle: create, update, soft-delete
- Business-scoped statistics and analytics

### 👥 Member Management
- Full CRUD for members with soft delete
- Member search by name or phone
- Member status filtering (ACTIVE / INACTIVE)
- Member self-registration and login portal
- Member-specific dashboard (active subscription, payment history)
- Bulk CSV import and export
- CSV import template download

### 🎫 Subscription Management
- Custom subscription plan creation per business
- Plan assignment to members
- Automatic subscription expiry via scheduled jobs
- Expiry reminders (7 days, 3 days, 1 day before expiry)
- Subscription history per member

### 💳 Payment Management
- Manual payment recording with multiple payment methods (Cash, UPI, Net Banking, Cards, Paytm, PhonePe, Google Pay, Razorpay, Stripe, Cheque, Bank Transfer)
- Payment history per member and per business
- Revenue analytics (total, monthly, daily)
- Payment method breakdown statistics
- CSV payment export

### 🧑‍💼 Staff Management
- Add staff directly or via email invitation
- Fine-grained roles: `OWNER`, `MANAGER`, `RECEPTIONIST`, `TRAINER`, `ACCOUNTANT`, `SALES`
- Per-role permission sets with override support (grant/revoke individual permissions)
- Staff invitation system with token-based acceptance
- Staff login with business context embedded in JWT
- Staff status management: Active, Suspended, On Leave, Terminated

### 📊 Dashboards
- **Owner Dashboard** — Members, revenue, subscriptions, payments, growth metrics
- **Staff Dashboard** — Daily tasks, expiring subscriptions, recent payments
- **Member Dashboard** — Active plan, days remaining, payment history

### 🔍 Audit Logging
- Full audit trail for all critical business actions
- Auth event logging (logins, failures, password changes, lockouts)
- Business-scoped audit log retrieval with pagination

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21+ |
| Framework | Spring Boot 3.3.5 |
| Security | Spring Security, JWT (JJWT) |
| ORM | Spring Data JPA / Hibernate |
| Database | PostgreSQL (recommended) / MySQL |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Build Tool | Maven |
| Scheduling | Spring `@Scheduled` |
| Async | Spring `@Async` with thread pool |
| CSV | Custom Java I/O |

---

## Architecture

Managio follows a **modular monolith** architecture with domain-driven package structure. Each domain module is self-contained with its own controller, service, repository, entity, and DTO layers.

```
┌──────────────────────────────────────────────────────────────┐
│                        REST API Layer                         │
│  (AuthController, BusinessController, MemberController, ...)  │
└────────────────────────────┬─────────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────────┐
│                      Security Filter Chain                    │
│         (JWT Auth Filter, Rate Limit Filter, CORS)            │
└────────────────────────────┬─────────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────────┐
│                       Service Layer                           │
│  AuthService │ BusinessService │ MemberService │ StaffService │
│  SubscriptionService │ PaymentService │ AuditLogService       │
└────────────────────────────┬─────────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────────┐
│                    Repository Layer (JPA)                     │
└────────────────────────────┬─────────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────────┐
│                     Database (PostgreSQL)                     │
└──────────────────────────────────────────────────────────────┘
```

---

## Getting Started

### Prerequisites

- **Java 21** or higher
- **Maven 3.9.11+**
- **PostgreSQL 18+** (or MySQL 8+)
- (Optional) Docker for database

### Installation

```bash
# Clone the repository
git clone https://github.com/your-username/managio-backend.git
cd managio-backend
```

### Configuration

Copy and edit the application properties:

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Set the following required properties:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/managio
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password

# JWT Secrets (minimum 32 characters each, must be different)
app.security.jwt.access-secret=your-super-secret-access-key-min-32-chars
app.security.jwt.refresh-secret=your-super-secret-refresh-key-min-32-chars

# Token Expiry
app.security.jwt.access-expiry-seconds=900
app.security.jwt.refresh-expiry-seconds=2592000
```

### Running the Application

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

Or run the JAR directly:

```bash
java -jar target/managio-backend-1.0.0.jar
```

The application will start on `http://localhost:8080`.

---

## API Documentation

Once running, Swagger UI is available at:

```
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON spec:

```
http://localhost:8080/v3/api-docs
```

Health check:

```
GET http://localhost:8080/api/health
```

---

## Module Breakdown

### Authentication — `/api/v1/auth`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/register` | Register a new user |
| POST | `/login` | Standard login |
| POST | `/staff/login` | Staff login with business context |
| POST | `/refresh` | Refresh access token |
| POST | `/logout` | Revoke refresh token |
| POST | `/verify-email` | Email verification |
| POST | `/forgot-password` | Request password reset |
| POST | `/reset-password` | Reset password |
| POST | `/change-password` | Change password |
| GET | `/me` | Get current user profile |

### Business — `/api/v1/businesses`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/` | Create a business |
| GET | `/my` | Get all businesses owned by current user |
| GET | `/{id}` | Get business by ID |
| PUT | `/{id}` | Update business |
| DELETE | `/{id}` | Soft-delete business |
| GET | `/{id}/statistics` | Business statistics |
| GET | `/{id}/staff` | List staff members |

### Members — `/api/v1/businesses/{businessId}/members`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/` | Create member |
| GET | `/` | List members (paginated) |
| GET | `/with-subscriptions` | List members with subscription info |
| GET | `/search?query=` | Search members |
| GET | `/status/{status}` | Filter by status |
| GET | `/{id}` | Get member by ID |
| GET | `/{id}/profile` | Full member profile |
| GET | `/{id}/subscription-history` | Subscription history |
| GET | `/{id}/payment-history` | Payment history |
| PUT | `/{id}` | Update member |
| POST | `/{id}/deactivate` | Deactivate member |
| GET | `/export` | Export members CSV |
| GET | `/import-template` | Download CSV template |
| POST | `/import` | Bulk import from CSV |

### Subscriptions — `/api/v1/businesses/{businessId}/subscriptions`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/plans` | Create subscription plan |
| GET | `/plans` | List active plans |
| POST | `/assign` | Assign plan to member |
| GET | `/count` | Count active subscriptions |

### Payments — `/api/v1/businesses/{businessId}/payments`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/` | Record a manual payment |
| GET | `/` | List all payments (paginated) |
| GET | `/member/{memberId}` | Member payment history |
| GET | `/stats` | Payment method statistics |
| GET | `/revenue/monthly` | Monthly revenue |
| GET | `/recent?days=7` | Recent payments |
| GET | `/export` | Export payments CSV |

### Staff — `/api/v1/businesses/{businessId}/staff`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/` | Add staff member |
| GET | `/` | List staff (paginated) |
| GET | `/list` | List staff (all) |
| GET | `/{id}` | Get staff by ID |
| GET | `/{id}/detail` | Detailed staff info |
| PUT | `/{id}` | Update staff |
| POST | `/{id}/terminate` | Terminate staff |
| POST | `/{id}/suspend` | Suspend staff |
| POST | `/{id}/activate` | Activate staff |
| POST | `/{staffId}/permissions/{permission}/grant` | Grant permission |
| POST | `/{staffId}/permissions/{permission}/revoke` | Revoke permission |
| GET | `/{staffId}/permissions` | Get effective permissions |

### Staff Invitations — `/api/v1/staff`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/businesses/{businessId}/invite` | Invite staff via email |
| POST | `/accept-invitation` | Accept invitation with token |
| GET | `/invitation?token=` | Get invitation by token |
| GET | `/businesses/{businessId}/invitations` | List invitations |
| POST | `/invitations/{id}/resend` | Resend invitation |
| DELETE | `/invitations/{id}` | Cancel invitation |

### Audit Logs — `/api/v1/businesses/{businessId}/audit-logs`

| Method | Endpoint | Description |
|---|---|---|
| GET | `/` | All audit logs (paginated) |
| GET | `/entity/{entityType}` | Filter by entity type |
| GET | `/recent?days=7` | Recent audit logs |

### Dashboards

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/businesses/{businessId}/dashboard/owner` | Owner dashboard |
| GET | `/api/v1/businesses/{businessId}/dashboard/staff` | Staff dashboard |
| GET | `/api/v1/members/{memberId}/dashboard` | Member dashboard |

### Member Self-Service — `/api/v1/members/auth`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/register` | Member self-registration |
| POST | `/login` | Member login |
| POST | `/change-password` | Change password |
| POST | `/forgot-password` | Request reset |

---

## Security

- **JWT Access Tokens** — Short-lived (15 min default), contain user ID, email, roles, and for staff: `staffId`, `businessId`, `staffRole`, and permission flags.
- **JWT Refresh Tokens** — Long-lived (30 days default), stored in DB with rotation on each use.
- **Token Blacklisting** — In-memory blacklist with scheduled cleanup (hourly).
- **Password Policy** — Minimum 8 characters, requires uppercase, lowercase, digit, and special character.
- **Account Lockout** — After 5 failed login attempts (configurable), account is locked for 30 minutes (configurable).
- **Rate Limiting** — Applied to `/login`, `/register`, `/forgot-password`, `/reset-password` endpoints.
- **Security Headers** — `X-Content-Type-Options`, `X-Frame-Options`, `X-XSS-Protection`, `Strict-Transport-Security`.
- **CORS** — Configurable allowed origins.

---

## Scheduled Jobs

| Schedule | Job | Description |
|---|---|---|
| Daily @ 00:00 | `SubscriptionExpiryScheduler` | Marks expired subscriptions as `EXPIRED` |
| Daily @ 09:00 | `ExpiryReminderScheduler` | Sends email reminders for subscriptions expiring in 7, 3, and 1 day |
| Daily @ 10:00 | Payment Confirmations | (Placeholder — coming in v2) |
| Hourly | Token Blacklist Cleanup | Removes expired tokens from in-memory blacklist |

---

## Project Structure

```
src/main/java/com/nitin/saas/
├── ManagioApplication.java
├── audit/
│   ├── controller/      # AuditLogController
│   ├── dto/
│   ├── entity/          # AuditLog
│   ├── repository/
│   └── service/         # AuditLogService
├── auth/
│   ├── controller/      # AuthController
│   ├── dto/
│   ├── entity/          # User, RefreshToken, EmailVerificationToken, PasswordResetToken, AuthAuditLog
│   ├── enums/           # Role
│   ├── repository/
│   └── service/         # AuthService, RBACService
├── business/
│   ├── controller/      # BusinessController
│   ├── dto/
│   ├── entity/          # Business
│   ├── repository/
│   └── service/         # BusinessService, BusinessStatisticsService
├── common/
│   ├── config/          # AsyncConfig, JacksonConfig, OpenApiConfig, WebMvcConfig, CookieSecurityConfig
│   ├── controller/      # HealthController
│   ├── email/           # EmailNotificationService
│   ├── exception/       # GlobalExceptionHandler, custom exceptions
│   ├── export/          # CSVExportService, BulkImportService
│   ├── security/        # JwtUtil, JwtAuthFilter, SecurityConfig, RateLimitFilter, etc.
│   └── utils/
├── dashboard/
│   ├── controller/      # OwnerDashboardController, StaffDashboardController, MemberDashboardController
│   ├── dto/
│   └── service/
├── member/
│   ├── controller/      # MemberController, MemberAuthController
│   ├── dto/
│   ├── entity/          # Member
│   ├── repository/
│   └── service/         # MemberService, MemberProfileService, MemberAuthService
├── payment/
│   ├── controller/      # PaymentController
│   ├── dto/
│   ├── entity/          # Payment
│   ├── enums/           # PaymentMethod
│   ├── repository/
│   └── service/         # PaymentService, PaymentStatsService
├── staff/
│   ├── controller/      # StaffController, StaffInvitationController
│   ├── dto/
│   ├── entity/          # Staff, StaffInvitation, StaffPermission
│   ├── enums/           # StaffRole
│   ├── repository/
│   └── service/         # StaffService, StaffAuthService, StaffInvitationService
└── subscription/
    ├── controller/      # SubscriptionController
    ├── dto/
    ├── entity/          # SubscriptionPlan, MemberSubscription
    ├── repository/
    ├── scheduler/       # SubscriptionExpiryScheduler, ExpiryReminderScheduler
    └── service/         # SubscriptionService
```

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `app.security.jwt.access-secret` | *(required)* | JWT access token signing secret (min 32 chars) |
| `app.security.jwt.refresh-secret` | *(required)* | JWT refresh token signing secret (min 32 chars) |
| `app.security.jwt.access-expiry-seconds` | `900` | Access token TTL (15 min) |
| `app.security.jwt.refresh-expiry-seconds` | `2592000` | Refresh token TTL (30 days) |
| `app.security.jwt.issuer` | `managio` | JWT issuer claim |
| `app.security.max-login-attempts` | `5` | Max failed logins before lockout |
| `app.security.account-lock-duration-minutes` | `30` | Account lock duration |
| `app.security.refresh-token-expiry-days` | `30` | Refresh token lifespan in days |
| `app.security.email-verification-expiry-hours` | `24` | Email verification token TTL |
| `app.security.password-reset-expiry-hours` | `1` | Password reset token TTL |
| `app.security.invitation-expiry-hours` | `72` | Staff invitation TTL |
| `app.oauth2.redirect-uri` | `http://localhost:3000/auth/callback` | OAuth2 redirect URI |
| `app.name` | `Managio` | Application name |
| `app.version` | `1.0.0` | Application version |

---

## Contributing

This is a v1 proprietary release. Internal contributions follow the standard Git flow:

1. Create a feature branch from `main`: `git checkout -b feature/your-feature`
2. Commit with clear messages
3. Open a pull request with a description of changes
4. Ensure all tests pass before requesting review

---

## License

Proprietary — © 2026 Managio. All rights reserved.

---

> Built with ❤️ using Spring Boot · Version 1.0.0
