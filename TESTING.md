# Testing Guide

Single reference for the safe-zone test suite: what exists, how to run it, and how it fits into CI.

---

## Overview

- **CI (Jenkins)** runs backend unit tests (user, product, media, order, eureka, api-gateway) and frontend unit tests (Karma/Jasmine). No controller-level integration tests with Testcontainers are active in CI.
- **Backend:** Maven Wrapper (`mvnw`). Shared module is built first, then each service runs its tests. JUnit XML and JaCoCo reports are produced.
- **Frontend:** Angular/Karma, ChromeHeadlessNoSandbox in CI. Coverage (LCOV) and JUnit XML for Jenkins. Optional E2E with Cypress.

---

## Quick commands

### Backend (from repo root)

```bash
cd backend

# Build shared then run all service tests (same order as Jenkins)
cd shared && ../mvnw clean install -DskipTests && cd ..
cd services/user    && ../../mvnw test && cd ../..
cd services/product && ../../mvnw test && cd ../..
cd services/media   && ../../mvnw test && cd ../..
cd services/order   && ../../mvnw test && cd ../..
cd services/eureka  && ../../mvnw test && cd ../..
cd api-gateway      && ../mvnw test
```

Or from backend root: `./mvnw test` (if your multi-module setup runs all modules).

**Single service / single class / coverage:**

```bash
cd backend/services/user
../../mvnw test
../../mvnw test -Dtest=UserServiceTest
../../mvnw test jacoco:report
# Report: target/site/jacoco/index.html
```

### Frontend

```bash
cd frontend
npm ci
npm run test                    # Headless, single run, coverage (CI-style)
npm run e2e                     # Cypress headless
npm run e2e:open                # Cypress UI
```

Note: There is no `test:ci` script; `npm run test` is already headless with coverage.

### Full local run (backend + frontend)

```bash
cd backend && ./mvnw test && cd ../frontend && npm ci && npm run test
```

---

## Backend test structure

| Service      | What runs in CI                         | Notes |
| ------------ | --------------------------------------- | ----- |
| User         | Unit / service tests                    | `AuthControllerIntegrationTest` exists but is commented out |
| Product      | Unit / service / listener tests         | No controller integration test class |
| Media        | Unit / event producer tests             | No controller integration test class |
| Order        | Unit / controller (MockMvc + mocks)     | `OrderControllerTest`, `OrderServiceTest`, etc. |
| Eureka       | @SpringBootTest smoke                   | `EurekaServerApplicationTests` |
| API Gateway  | Unit tests                              | See `backend/api-gateway` |

**Key paths:**

- Tests: `backend/services/*/src/test/java/**/*Test.java`
- JUnit XML (CI): `backend/**/target/surefire-reports/*.xml`
- JaCoCo: `backend/services/*/target/site/jacoco/index.html`
- Shared test util (JWT): `backend/shared/src/main/java/com/buyapp/common/test/JwtTestUtil.java`

**Integration tests (future):** Controller-level tests with Testcontainers + MockMvc + `JwtTestUtil` are not active. When added, use the same pattern as the commented `AuthControllerIntegrationTest` (Testcontainers MongoDB, `@DynamicPropertySource` for `spring.data.mongodb.uri`). See [Testcontainers](https://www.testcontainers.org/) and [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing).

---

## Frontend test structure

- **Unit:** `frontend/src/app/**/*.spec.ts` (components, services). Karma config: `frontend/karma.conf.js` (ChromeHeadlessNoSandbox, JUnit output under `frontend/test-results/`).
- **E2E:** `frontend/cypress/e2e/**/*.cy.ts` (e.g. `critical-path.cy.ts`). Run with dev server or against deployed app; tests can use `cy.intercept()` for API mocking.
- **Coverage:** `frontend/coverage/` (LCOV for SonarQube). CI uses `coverage/lcov.info`.

---

## CI (Jenkins)

Pipeline is in `Jenkinsfile`. Main test-related stages:

1. **Checkout**
2. **Backend Tests** — build shared, then `../../mvnw test` in each service (user, product, media, order, eureka, api-gateway).
3. **Frontend Tests** — `npm ci`, `npm run test` (15 min timeout).
4. **SonarQube** — backend JaCoCo + frontend LCOV; then quality gate.

Artifacts: `backend/**/target/surefire-reports/*.xml`, `frontend/test-results/*.xml`.

---

## Troubleshooting

- **Docker (Testcontainers):** If you enable integration tests, ensure Docker is running (`docker ps`). First run may pull images (e.g. `mongo:7.0`).
- **Port 27017:** Usually not an issue; Testcontainers assigns ports. If needed: `lsof -ti:27017 | xargs kill -9`.
- **Maven OOM:** `export MAVEN_OPTS="-Xmx2048m"` then re-run.
- **Frontend timeouts:** See `karma.conf.js` (`captureTimeout`, `browserNoActivityTimeout`). Jenkins allows 15 min for the frontend test stage.
- **Chrome in CI:** `CHROME_BIN=/usr/bin/chromium` is set in Jenkins; the image must provide Chromium.

---

## References

- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [Testcontainers](https://www.testcontainers.org/)
- [JUnit 5](https://junit.org/junit5/docs/current/user-guide/)
- [Jasmine](https://jasmine.github.io/)
- [Cypress](https://www.cypress.io/)
