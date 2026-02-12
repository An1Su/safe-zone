# E-commerce Platform (safe-zone)

Full-stack e-commerce marketplace built with Spring Boot microservices and Angular 21. Covers the full flow: browse → cart → checkout (Pay on Delivery) → order tracking, plus user/seller profiles with analytics and product search/filtering. Includes JWT auth, role-based access, CI/CD (Jenkins), and SonarQube quality gates.

## Features

**Backend (Spring Boot Microservices)**

- **User** — Registration, login/logout, CLIENT/SELLER roles, JWT with token blacklisting
- **Product** — CRUD with ownership, search/filter (keyword, facets), my-products
- **Media** — Image upload (2MB limit), product/avatar images
- **Order** — Cart (add/update/remove), create order with Pay on Delivery, list/search orders, cancel/redo; seller order management and status updates
- **Eureka** — Service discovery
- **API Gateway** — Routing, auth, WebFlux
- **Kafka** — Event-driven updates across services

**Frontend (Angular 21)**

- Product browsing, search with filters and pagination
- **Cart** — List, edit quantity, remove, subtotal/total
- **Checkout** — Address → review → confirm (Pay on Delivery)
- **Orders** — List with search (status/date), order details with status timeline
- **User profile** — Total spent, most bought, top categories (charts)
- **Seller dashboard** — Revenue, best-selling products, units sold (charts)
- Route guards, HTTP interceptors, responsive SCSS

**Security & quality**

- BCrypt, HTTPS (self-signed in dev), HttpOnly cookies + localStorage, role-based access, file validation
- **CI/CD**: Jenkins pipeline (test → SonarQube quality gate → build → deploy). PR workflows, branch protection with green pipeline.
- **Code quality**: SonarQube; issues addressed in PRs.

## Quick Start with Docker (Recommended)

### Prerequisites

- Docker and Docker Compose
- 8GB+ RAM recommended

### Setup

```bash
# 1. Generate SSL certificates (first time only)
./generate-ssl-certs.sh

# 2. Build all services
./docker-build.sh

# 3. Start all containers
docker-compose up -d

# 4. Verify health (wait ~30 seconds)
docker-compose ps
```

### Access Points

- **Frontend**: https://localhost:4200 (Angular app)
- **API Gateway**: https://localhost:8080 (REST API)
- **Eureka Dashboard**: http://localhost:8761 (Service registry)
- **MongoDB**: localhost:27017 (admin/password)
- **Kafka**: localhost:9092

**Note**: Accept the self-signed certificate warning in your browser (Advanced → Proceed).

### Stop Services

```bash
docker-compose down
```

## Manual Setup (without Docker)

See detailed instructions in:

- [Backend README](backend/README.md) - Backend services setup
- [Frontend README](frontend/README.md) - Frontend setup

## Architecture

### Microservices

```
┌─────────────┐
│   Angular   │ :4200 (nginx)
│  Frontend   │
└──────┬──────┘
       │ HTTPS
┌──────▼──────────┐
│  API Gateway    │ :8080 (Spring Cloud Gateway)
│   + Eureka      │ :8761 (Service Discovery)
└────────┬────────┘
         │
    ┌────┴────┬────────┬────────┬─────────┐
    │         │        │        │         │
┌───▼───┐ ┌──▼───┐ ┌──▼────┐ ┌──▼────┐ ┌──▼──────┐
│ User  │ │Product│ │ Media │ │ Order │ │  Kafka  │
│ :8081 │ │ :8082 │ │ :8083 │ │ :8085 │ │  :9092  │
└───────┘ └───────┘ └───────┘ └───────┘ └─────────┘
    └──────────────┴────────────┘
              MongoDB :27017
```

### Microservices overview

| Service             | Port  | Role                                                                  |
| ------------------- | ----- | --------------------------------------------------------------------- |
| **api-gateway**     | 8080  | Routing, JWT validation, HTTPS; all client requests go here           |
| **eureka-server**   | 8761  | Service discovery (register/lookup user, product, media, order)       |
| **user-service**    | 8081  | Auth (register, login, logout), user profile, CLIENT/SELLER           |
| **product-service** | 8082  | Product CRUD, search/filter, my-products, ownership                   |
| **media-service**   | 8083  | Image upload (2MB), product/avatar images                             |
| **order-service**   | 8085  | Cart, orders, Pay on Delivery, order search, seller order management  |
| **kafka**           | 9092  | Event bus (user/product/media events)                                 |
| **mongodb**         | 27017 | Persistence (user, product, media, order each use own DB/collections) |
| **frontend**        | 4200  | Angular SPA (nginx in Docker)                                         |

All backend services register with Eureka. The gateway routes by path (e.g. `/auth/*` → user-service, `/products/*` → product-service, `/media/*` → media-service, `/cart`, `/orders` → order-service).

### Technology Stack

| Layer       | Technologies                                   |
| ----------- | ---------------------------------------------- |
| Frontend    | Angular 21, TypeScript, SCSS, RxJS             |
| API Gateway | Spring Cloud Gateway, WebFlux                  |
| Services    | Spring Boot 3.x, Java 17+                      |
| Database    | MongoDB with authentication                    |
| Messaging   | Kafka (KRaft mode)                             |
| Security    | JWT, BCrypt, HTTPS, HttpOnly cookies           |
| CI/CD       | Jenkins (test → quality gate → build → deploy) |
| Quality     | SonarQube                                      |
| Container   | Docker Compose, nginx (Alpine)                 |

## API Endpoints

### Authentication

- `POST /auth/register` - Register new user (CLIENT or SELLER)
- `POST /auth/login` - Login and receive JWT
- `POST /auth/logout` - Logout and blacklist token

### Products (Public)

- `GET /products` - List all products
- `GET /products/{id}` - Get product details

### Products (Authenticated)

- `POST /products` - Create product (SELLER only)
- `PUT /products/{id}` - Update product (owner only)
- `DELETE /products/{id}` - Delete product (owner only)
- `GET /products/my-products` - Get user's products

### Media

- `POST /media/upload/{productId}` - Upload image (SELLER, 2MB max)
- `GET /media/product/{productId}` - Get product images
- `DELETE /media/{mediaId}` - Delete image (owner only)

### Cart & Orders (Order Service)

- `GET /cart` - Get current user's cart
- `POST /cart/items` - Add item to cart
- `PUT /cart/items/{productId}` - Update quantity
- `DELETE /cart/items/{productId}` - Remove item
- `DELETE /cart` - Clear cart
- `POST /orders` - Create order (Pay on Delivery, from cart)
- `GET /orders` - List user's orders
- `GET /orders/{id}` - Order details
- `PUT /orders/{id}/cancel` - Cancel order
- `POST /orders/{id}/redo` - Redo cancelled order
- `GET /orders/search` - Search user orders (status, date)
- `GET /orders/seller` - Seller's orders
- `GET /orders/seller/{id}` - Seller order details
- `PUT /orders/{id}/status` - Update order status (SELLER)
- `GET /orders/seller/search` - Search seller orders

## Database Schema

### Users

```json
{
  "_id": "ObjectId",
  "name": "string",
  "email": "string (unique)",
  "password": "string (BCrypt hashed)",
  "role": "CLIENT | SELLER",
  "avatar": "string (optional)"
}
```

### Products

```json
{
  "_id": "ObjectId",
  "name": "string",
  "description": "string",
  "price": "number",
  "stock": "number",
  "userId": "string (User._id)",
  "user": "string (User.name)"
}
```

### Media

```json
{
  "_id": "ObjectId",
  "fileName": "string",
  "filePath": "string",
  "contentType": "string",
  "fileSize": "number",
  "productId": "string (Product._id)",
  "sellerId": "string (User._id)"
}
```

### Carts (Order Service DB)

Collection: `carts`. One cart per user.

```json
{
  "_id": "ObjectId",
  "userId": "string (User._id)",
  "items": [
    {
      "productId": "string (Product._id)",
      "productName": "string",
      "quantity": "number (≥ 1)",
      "price": "number (> 0)"
    }
  ],
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```

### Orders (Order Service DB)

Collection: `orders`.

```json
{
  "_id": "ObjectId",
  "userId": "string (User._id)",
  "items": [
    {
      "productId": "string",
      "productName": "string",
      "sellerId": "string (User._id)",
      "quantity": "number",
      "price": "number"
    }
  ],
  "status": "PENDING | READY_FOR_DELIVERY | SHIPPED | DELIVERED | CANCELLED",
  "totalAmount": "number",
  "shippingAddress": {
    "fullName": "string",
    "address": "string",
    "city": "string",
    "phone": "string"
  },
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```

**Order status flow:** `PENDING` → `READY_FOR_DELIVERY` → `SHIPPED` → `DELIVERED`. `CANCELLED` is allowed from `PENDING` or `READY_FOR_DELIVERY`.

## Testing

Backend unit tests (all services including order, eureka, api-gateway) and frontend unit tests (Karma/Jasmine) run in Jenkins. No controller-level integration tests with Testcontainers are currently active.

```bash
# Backend (from backend root)
cd backend && ./mvnw test

# Frontend
cd frontend && npm ci && npm run test
```

See [TESTING.md](TESTING.md) for full commands, coverage, and CI details.

### Manual Testing

```bash
# Register seller
curl -X POST https://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"John","email":"john@example.com","password":"pass123","role":"SELLER"}'

# Login and save token
TOKEN=$(curl -X POST https://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"pass123"}' | jq -r '.token')

# Create product
curl -X POST https://localhost:8080/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Product","description":"Test","price":19.99,"stock":100}'

# View all products (no auth)
curl https://localhost:8080/products
```

## Project Structure

```
safe-zone/
├── backend/
│   ├── api-gateway/         # Spring Cloud Gateway
│   ├── services/
│   │   ├── eureka/          # Service discovery
│   │   ├── user/            # User management, auth
│   │   ├── product/         # Product CRUD, search
│   │   ├── media/           # Image upload/storage
│   │   └── order/           # Cart, orders, Pay on Delivery
│   └── shared/              # DTOs, JWT util, exceptions
├── frontend/                # Angular 21 SPA
│   ├── src/app/
│   │   ├── components/      # Cart, checkout, orders, profiles, search, products, auth
│   │   ├── services/        # API services
│   │   ├── guards/          # Auth, role guards
│   │   └── interceptors/    # Token, 401/403
│   ├── cypress/             # E2E tests
│   ├── nginx.conf           # Docker reverse proxy
│   └── Dockerfile
├── jenkins/                 # Jenkins Docker setup
├── sonarqube/               # SonarQube config
├── Jenkinsfile              # CI: test → quality gate → build → deploy
├── docker-compose.yml
└── README.md
```

## CI/CD

- **Jenkins** (`Jenkinsfile`): Checkout → Backend tests → Frontend tests → SonarQube analysis → Quality gate → Build (Docker) → Deploy (main branch). Use feature branches and PRs; protect `main` with approved reviews and green pipeline.
- **SonarQube**: Backend (JaCoCo) and frontend (LCOV). Address quality gate issues in PRs. See `sonarqube/` and `jenkins/` for setup.

## Documentation

- **[backend/README.md](backend/README.md)** — Backend services, API, setup
- **[frontend/README.md](frontend/README.md)** — Angular app, components, routing
- **[TESTING.md](TESTING.md)** — How to run tests (backend, frontend, CI)
- **[CONTRIBUTING.md](CONTRIBUTING.md)** — Branch naming, PR workflow
- **[B01-Task.md](B01-Task.md)**, **[MJ-Task.md](MJ-Task.md)** — Project task specs

## Known Issues

1. **SSL Certificate Warning**: Self-signed certificate triggers browser warning (normal for development). Click "Advanced" → "Proceed to localhost".
2. **File Storage**: Images stored locally at `backend/services/media/uploads/images/`. Use cloud storage (S3, Cloudinary) for production.
3. **Startup Time**: First startup takes ~30-60 seconds for service registration and health checks.

## Development Scripts

**Used by Jenkins (CI):** The pipeline runs only `./generate-ssl-certs.sh` when needed (Build stage, if `frontend/ssl/localhost-cert.pem` is missing). Build and deploy use `docker-compose` directly.

**Local development (not called by Jenkins):**

```bash
./generate-ssl-certs.sh   # Generate self-signed SSL certificates (run once; also used in CI if missing)
./docker-build.sh         # Build all Docker images
./docker-start.sh         # Start all containers
./stop-all.sh             # Stop all containers
# Tests: see TESTING.md (e.g. cd backend && ./mvnw test; cd frontend && npm run test)
```

**Other scripts in the repo (optional / manual use):** `./start-all.sh`, `./build-all.sh`, `./seed-database.sh`, `backend/scripts/seed-data.sh`. These are not invoked by the Jenkins pipeline.

## Contributing

1. Fork the repository
2. Create feature branch: `git checkout -b feature/my-feature`
3. Commit changes: `git commit -m 'feat: add feature'`
4. Push to branch: `git push origin feature/my-feature`
5. Submit Pull Request

See [CONTRIBUTING.md](CONTRIBUTING.md) for branch naming conventions.

---

**Built with ❤️ using Spring Boot and Angular**
