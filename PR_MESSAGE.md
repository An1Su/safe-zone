# Fix: Frontend Tests and Docker Healthcheck Failures

## Summary

This PR fixes two critical issues that were causing CI/CD pipeline failures:

1. **Frontend test failures** - CartService tests failing due to unhandled HTTP requests
2. **Docker deployment failures** - Healthcheck failures due to missing `curl` in Docker images

## Changes Made

### Frontend Tests (`frontend/src/app/services/cart.service.spec.ts`)

- **Issue**: CartService constructor was auto-loading cart when `isLoggedIn()` returned `true`, causing unhandled HTTP requests in tests
- **Fix**: Set `authService.isLoggedIn()` to return `false` by default in test setup to prevent auto-loading during service initialization
- **Impact**: All 55 frontend tests now pass successfully

### Docker Healthchecks (Multiple Dockerfiles)

- **Issue**: Healthcheck commands using `curl` were failing because `curl` was not installed in Docker images
- **Fix**: Added `curl` installation to all service Dockerfiles:
  - `backend/services/user/Dockerfile`
  - `backend/services/product/Dockerfile`
  - `backend/services/order/Dockerfile`
  - `backend/services/media/Dockerfile`
  - `backend/api-gateway/Dockerfile`
- **Impact**: Healthchecks now execute successfully, allowing services to start properly in Docker Compose

## Testing

- ✅ All frontend tests passing (55/55)
- ✅ Backend tests passing
- ✅ Docker containers can now pass healthchecks and start successfully

## Related Issues

- Fixes Jenkins frontend test stage failures
- Fixes Jenkins deployment stage failures (unhealthy containers)

## Notes

- The `eureka-server` Dockerfile already had `curl` installed, so no changes were needed
- All Dockerfiles follow the same pattern: install `curl` and clean up apt cache to minimize image size
