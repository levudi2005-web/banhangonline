# Runtime Validation Report

**Generated**: 2026-10-05
**Target**: `C:\Users\USaD\Documents\banhangonline`

## Summary

| Step | Status | Exit Code | Details |
|---|---|---:|---|
| Isolated startup | PASS | n/a | E2E Spring Boot app on `127.0.0.1:8081`; datasource/JPA startup verified; root returned HTTP 200 |
| Gated auth and fixture integration tests | PASS | 0 | 11 tests, 0 failures, 0 errors, 0 skipped |
| Standard clean Java tests | PASS | 0 | 65 tests, 0 failures, 0 errors, 11 skipped |
| Executable package | PASS | 0 | `backend/target/app.jar` packaged successfully |
| JavaScript syntax | PASS | 0 | 15 JavaScript files checked, 0 syntax failures |
| Browser E2E | PASS (manual Playwright session) | n/a | Real Edge browser exercised against the isolated running application; no retained automated spec/report or runner exit code |
| Git whitespace check | PASS | 0 | `git diff --check` |

**Overall**: PARTIAL — isolated customer, management, permission, privacy, and order flows were exercised, but concurrent/delayed duplicate-checkout behavior was not established, browser runs were not saved as a repeatable spec, and no deployment was performed.

## Environment

- Docker daemon was unavailable; the E2E database was a separate MariaDB 11.4 instance bound to loopback port 3307, using database `banhangonline_e2e`.
- Node.js and Playwright were available; Playwright 1.63.0 used installed Edge 154.
- The E2E Spring profile uses `application-e2e.properties`, binds the app to `127.0.0.1:8081`, requires E2E database credentials from environment variables, and uses Hibernate validation only.
- The configured TiDB datasource was not used for fixture writes, price changes, cart operations, or orders.
- Hibernate/JPA initialized successfully, although startup emitted a metadata warning involving `Unknown column 'RESERVED' in 'WHERE'`; this was not resolved by the tests.

## Test Evidence

### Integration

Command:

```text
mvn "-Dspring.profiles.active=e2e" "-DrunLiveDbTests=true" "-Dtest=AuthIntegrationTest,E2eFixtureSeederTest" -f backend/pom.xml test
```

Exit code: 0. `AuthIntegrationTest`: 10 passed, 0 skipped. `E2eFixtureSeederTest`: 1 passed, 0 skipped.

The standard regression command `mvn -f backend/pom.xml clean test` exited 0: 65 tests, 0 failures, 0 errors, 11 skipped. The 10 `AuthIntegrationTest` cases and the fixture seeder are guarded by `@EnabledIfSystemProperty(named = "runLiveDbTests", matches = "true")`; the normal command omits that property. The tests were run separately with the property enabled against the isolated E2E profile.

### Browser and API

Real-browser/API/database checks included:

- Customer login and session cookie; storefront search, category filtering, sorting, product details, store switching, and pickup availability.
- Cart add, quantity updates, remove, clear, reload persistence, and empty state.
- Snapshot price invariant: after adding at 1000 VND, the product's live price was changed to 2000 VND in the isolated DB; cart and resulting order remained at the 1000 VND snapshot.
- Click-and-collect checkout returned 201, displayed the confirmation/pickup code after the UI fix, and cleared the cart. A sequential retry with an empty cart returned 400 and did not create a second order.
- Owner management pages and staff permission-aware navigation; backend authorization rejected forbidden operations and cross-store access.
- Customer order and notification isolation, invalid order transitions, cancellation rules, and pickup-code rejection.
- Clean storefront/public-browser runs reported no uncaught page errors, failed requests, or unexpected non-2xx responses.

Limitations: concurrent double-click or delayed-response checkout retries were not verified; registration/logout were covered by the gated auth integration suite, not all repeated through the browser; the browser scenarios were not retained as automated Playwright spec files.

### Build and Static Checks

- `mvn -f backend/pom.xml -DskipTests package`: exit code 0.
- `node --check` across 15 tracked/untracked JavaScript files: 0 failures.
- `git diff --check`: exit code 0.

## Test Data

The gated fixture test verifies the datasource identity before clearing and seeding tables. It requires the database name, MariaDB server identity, port, and JDBC URL to match the dedicated E2E database. Test identities and fixture rows remain only in that isolated database; a reset/reseed restored the deterministic baseline after browser scenarios. No TiDB test account, order, price, or inventory mutation was made.

## Outstanding

- Resolve or investigate the Hibernate metadata warning.
- Add retained, repeatable Playwright specs and run concurrent/delayed duplicate-checkout scenarios.
- No commit, push, or Render deployment was made.
