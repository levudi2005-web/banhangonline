# Copilot instructions

## Build and test

- Build the executable JAR: `mvn -f backend/pom.xml -DskipTests package`
- Run unit tests: `mvn -f backend/pom.xml test`
- Run a live TiDB integration test explicitly: `mvn -f backend/pom.xml -DrunLiveDbTests=true -Dtest=AuthIntegrationTest#login_wrongPassword_isRejected test`
- Run the application locally: `mvn -f backend/pom.xml spring-boot:run`

The app requires `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, and `DB_SSL_MODE`; there is no local or H2 datasource fallback. Resend's email API configuration (`RESEND_API_KEY`, `RESEND_FROM_EMAIL`, and `RESEND_FROM_NAME`) and OTP settings remain for the dormant OTP infrastructure; customer registration and password recovery currently do not use email OTP. Password recovery and account profile updates verify the registered phone's last four digits on the backend. Hibernate validates but never creates or updates schema. Flyway is disabled; schema deployment is managed outside the app from the approved database design. Email OTP schema changes are in `database/sql/01-auth/003_add_email_verification_flag.sql` and `004_create_verification_codes.sql`; they are manual and are never run by Spring. Unit tests do not need a database. `AuthIntegrationTest` is opt-in with `-DrunLiveDbTests=true` because it writes synthetic auth fixtures to the configured live database; do not enable it against real data without explicit approval. The project has no configured lint command.

## Architecture

- This is a Java 25 / Spring Boot application built with Maven. `BanHangOnlineApplication` starts the app, enables configuration-property scanning, and schedules maintenance tasks.
- The backend is under `backend/` and groups the implemented auth, user, address, role, permission, and store-registration code by feature. Controllers bind and validate DTOs, services implement transactional business rules, repositories access JPA entities, and `GlobalExceptionHandler` maps API and persistence errors to the shared `ApiResponse` envelope.
- Authentication is stateless at the Spring Security layer. Auth-owned session code under `auth/security` issues and resolves an HttpOnly `SID` cookie; only a SHA-256 hash of its token is stored in the `sessions` table. `SecurityConfig` installs those session components and the shared request-header and rate-limit filters under `common/security`.
- Customer and staff authentication endpoints live under `/api/auth`. Staff-store registration creates a pending account with no role; owner activation and role assignment are a manual database operation documented in `docs/approve-owner.sql`.
- Frontend pages and shared assets live under `frontend/` by audience and responsibility. Maven packages them into Spring's `static` resources; the shared JavaScript submits forms marked with `data-endpoint` as JSON and includes `X-Requested-With`.
- Historical Flyway SQL in `backend/src/main/resources/db/migration` documents the current auth schema and reference seed, but Flyway is not a runtime dependency and is disabled. The intended schema source is PowerDesigner MCD → MLD → MPD → reviewed SQL under `database/sql/`, manually applied only after review. Hibernate is validation-only.
- The current implementation is authentication, user, address, role/permission, and store registration only. `archive/` contains an older auth UI and an unrelated standalone Java sample; neither is built or served by the main application.

## Repository-specific conventions

- Keep API responses in the `ApiResponse` shape. Use validated request DTOs at controller boundaries and `UserResponse` for user data returned to clients; do not expose entity password hashes.
- Every state-changing request under `/api/` must include `X-Requested-With`. This custom check is the CSRF defense used here; the frontend already sends the header.
- Session and password-reset tokens are stored as hashes, not raw tokens. Preserve the generic response for password-reset requests so account existence is not disclosed.
- OTP infrastructure remains available but is dormant in current user-facing registration and recovery flows. If re-enabled, keep six-digit `SecureRandom` values stored only as BCrypt hashes; email destination identifiers use HMAC-SHA-256; preserve expiry, one-use semantics, attempt limits, cooldowns and send quotas. Store owners still require manual approval. Never log codes or provider credentials.
- Do not add H2 or a local datasource fallback. Keep the TiDB JDBC configuration based on `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, and `DB_SSL_MODE`. Never enable schema creation/update or run the historical Flyway scripts automatically. Reconcile SQL changes with the PowerDesigner model before manual deployment; existing V1/V2 remain historical references, and no native PowerDesigner model is present yet.
- Keep cookie, CORS, session, reset-token, and rate-limit settings in `application.properties` / `AppProperties`; deployment-specific values are supplied through environment variables (see `.env.example`).
- The commerce target is click-and-collect, not delivery. When order functionality is added, keep order lifecycle transitions authoritative in the order service, and separate customer-to-store chat from customer-to-AI assistance.
- Enforce privileged operations server-side. Any AI integration must call authenticated Java APIs through explicitly approved tools; never give a model unrestricted database access or executable arbitrary SQL.
- Do not create empty modules for planned features. Establish their data ownership and API/business boundaries when implementing those features; do not model category, product, store, and inventory as a forced dependency chain.
- When orders are implemented, the order service owns status transitions (`PENDING` → `CONFIRMED` → `PREPARING` → `READY_FOR_PICKUP` → `COMPLETED`), validates cancellations, and records status history and required notifications. Do not duplicate order-state rules in controllers, frontend, chat, or AI.
- Keep customer, staff, and owner permissions enforced by backend services/security; never authorize privileged actions based only on client-supplied roles.
