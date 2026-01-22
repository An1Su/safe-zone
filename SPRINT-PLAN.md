# 🎯 2-Week Sprint Plan: Decorative Makeup Store

**Project:** Final E-commerce Platform  
**Team Size:** 2 developers  
**Duration:** 2 weeks (10 working days)  
**Start Date:** _____________

---

## Executive Summary

This plan extends our existing buy-01/mr-jenk/safe-zone codebase to add the final e-commerce features: shopping cart, orders, checkout (Pay on Delivery), search/filtering, and user/seller analytics dashboards.

---

## Current State ✅ (Already Implemented)

| Component | Status |
|-----------|--------|
| User Service (auth, roles: CLIENT/SELLER) | ✅ Done |
| Product Service (CRUD) | ✅ Done |
| Media Service (image upload, 2MB limit) | ✅ Done |
| API Gateway + Eureka Discovery | ✅ Done |
| Kafka messaging | ✅ Done |
| Jenkins CI/CD + SonarQube | ✅ Done |
| Auth pages (Login/Register) | ✅ Done |
| Product listing & detail pages | ✅ Done |
| Seller dashboard (product management) | ✅ Done |
| Basic user profile | ✅ Done |
| Cart service (localStorage) | ✅ Started |

---

## Features to Build 🔨

| Feature | Priority | Owner |
|---------|----------|-------|
| Order Microservice (backend) | Must-have | Person A |
| Cart page + Checkout flow | Must-have | Person B |
| Order list & details pages | Must-have | Person B |
| Product search & filtering | Must-have | Both |
| User analytics dashboard | Must-have | Person B |
| Seller analytics dashboard | Must-have | Person B |
| Backend tests for orders | Must-have | Person A |
| Frontend tests | Must-have | Person B |

---

## Team Roles

### 👤 Person A (Backend-focused)
- Order microservice creation
- REST APIs for orders and analytics
- Search endpoint on Product Service
- Backend unit/integration tests
- API documentation

### 👤 Person B (Frontend-focused)
- Cart and checkout UI
- Order list and detail pages
- Search bar and filters UI
- User/Seller dashboard analytics sections
- Frontend tests and UI polish

---

## Database Changes 🗄️

### Existing Collections (No Changes Needed)

| Collection | Service | Status |
|------------|---------|--------|
| `users` | User Service | ✅ Keep as-is |
| `products` | Product Service | ✅ Keep as-is |
| `media` | Media Service | ✅ Keep as-is |
| `avatars` | Media Service | ✅ Keep as-is |

### New Collection: `orders`

**Owner:** Person A (Day 1)

```
orders
├── _id: ObjectId (auto)
├── userId: String (buyer's user ID)
├── status: String (PENDING | CONFIRMED | SHIPPED | DELIVERED | CANCELLED)
├── totalAmount: Double
├── shippingAddress: {
│   ├── fullName: String
│   ├── address: String
│   ├── city: String
│   └── phone: String
│ }
├── items: [
│   ├── productId: String
│   ├── productName: String (snapshot at purchase time)
│   ├── sellerId: String
│   ├── price: Double (snapshot at purchase time)
│   └── quantity: Integer
│ ]
├── createdAt: DateTime
└── updatedAt: DateTime
```

### Java Models to Create (Day 1)

| File | Type | Notes |
|------|------|-------|
| `Order.java` | `@Document` | Main entity with `@Id` |
| `OrderItem.java` | Embedded | No `@Document`, stored inside Order |
| `ShippingAddress.java` | Embedded | No `@Document`, stored inside Order |
| `OrderStatus.java` | Enum | PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED |

### Frontend Models to Create (Day 1)

| File | Notes |
|------|-------|
| `order.model.ts` | Order, OrderItem, ShippingAddress, OrderStatus interfaces |

### Recommended Indexes

```javascript
// For query performance (optional, can add later)
db.orders.createIndex({ "userId": 1 })           // find user's orders
db.orders.createIndex({ "items.sellerId": 1 })   // find seller's orders
db.orders.createIndex({ "createdAt": -1 })       // sort by date
```

> **Note:** MongoDB auto-creates the collection on first insert. No migration scripts needed.

---

## Week 1: Core Features (Days 1-5)

### Day 1 (Monday) — Setup & Database Models

| Person | Tasks | Est. Hours |
|--------|-------|------------|
| **A** | Create `order-service` scaffold (copy structure from product-service) | 2h |
| **A** | **DATABASE:** Create Java models: `Order.java`, `OrderItem.java`, `ShippingAddress.java`, `OrderStatus.java` (see Database Changes section above) | 1.5h |
| **A** | Create `OrderRepository.java` extending MongoRepository | 0.5h |
| **A** | Add order-service to `docker-compose.yml` and API Gateway routes | 1h |
| **B** | **DATABASE:** Create `order.model.ts` with TypeScript interfaces | 0.5h |
| **B** | Create `cart.component.ts/html/scss` — display cart items from CartService | 2.5h |
| **B** | Implement quantity controls (+/-), remove item, show subtotals | 2h |
| **Both** | Create feature branch `feature/cart-orders`, agree on Order DTO structure | 0.5h |

**Deliverable:** Order service compiles with models, `orders` collection ready (auto-created on first insert), cart page displays items

---

### Day 2 (Tuesday) — Order Backend Core

| Person | Tasks | Est. Hours |
|--------|-------|------------|
| **A** | Create `OrderController` with `POST /orders` (create order from cart) | 2h |
| **A** | Implement `GET /orders` (user's orders), `GET /orders/{id}` | 2h |
| **A** | Create `OrderRepository`, `OrderService` with business logic | 2h |
| **A** | Add order-service routes to API Gateway | 1h |
| **B** | Wire cart page to CartService, add "Proceed to Checkout" button | 2h |
| **B** | Style cart page (responsive, clean layout) | 2h |

**Deliverable:** Orders can be created via API, cart UI functional

---

### Day 3 (Wednesday) — Checkout Flow

| Person | Tasks | Est. Hours |
|--------|-------|------------|
| **A** | Add `OrderStatus` enum: `PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED` | 1h |
| **A** | Implement `PUT /orders/{id}/cancel` endpoint | 2h |
| **A** | Add validation: user can only cancel own orders, only if PENDING/CONFIRMED | 1h |
| **B** | Create `checkout.component` with 3 steps: | 4h |
|        | 1. Shipping address form (name, address, city, phone) | |
|        | 2. Order review (items, totals) | |
|        | 3. Confirm "Pay on Delivery" button | |
| **B** | Call `POST /orders` on confirm, show success message | 1h |
| **B** | Add routes: `/cart`, `/checkout` to `app.routes.ts` | 0.5h |

**Deliverable:** Complete checkout flow working end-to-end

---

### Day 4 (Thursday) — Order List Pages

| Person | Tasks | Est. Hours |
|--------|-------|------------|
| **A** | Implement `GET /orders/seller` — orders containing seller's products | 3h |
| **A** | Include product details in order response (join with Product Service) | 2h |
| **B** | Create `order-list.component` — list user's orders | 2h |
| **B** | Add status badges (color-coded), order date, total, link to details | 1h |
| **B** | Create `order-detail.component` — show items, totals, status | 2h |
| **B** | Add cancel button (calls `PUT /orders/{id}/cancel`) | 1h |

**Deliverable:** Users can view their order history and details

---

### Day 5 (Friday) — Search & Integration

| Person | Tasks | Est. Hours |
|--------|-------|------------|
| **A** | Add search endpoint to Product Service: | 4h |
|        | `GET /products/search?q={keyword}&minPrice=&maxPrice=&sort=` | |
| **A** | Implement MongoDB text search or regex on name/description | |
| **B** | Create seller orders view — list orders containing their products | 3h |
| **B** | Add basic status filter dropdown | 1h |
| **Both** | Code review, create PR, merge to main | 1h |
| **Both** | Verify Jenkins pipeline passes (tests + SonarQube) | 1h |

**Deliverable:** Week 1 complete, PR merged, pipeline green

---

## Week 2: Analytics & Polish (Days 6-10)

### Day 6 (Monday) — Search Frontend + Analytics Backend

| Person | Tasks | Est. Hours |
|--------|-------|------------|
| **A** | Create analytics endpoints: | 4h |
|        | `GET /orders/stats/user` — total spent, order count | |
|        | `GET /orders/stats/seller` — revenue, units sold | |
| **B** | Update `product-list.component`: | 4h |
|        | Add search bar (keyword input) | |
|        | Add price range filters (min/max inputs) | |
|        | Add sort dropdown (price low-high, high-low) | |
|        | Call search API on filter change | |

**Deliverable:** Product search functional, stats endpoints ready

---

### Day 7 (Tuesday) — User Dashboard Analytics

| Person | Tasks | Est. Hours |
|--------|-------|------------|
| **A** | Extend user stats endpoint: | 3h |
|        | Add "most bought products" (aggregate from orders) | |
|        | Add order count by status | |
| **A** | Write API tests for stats endpoints | 2h |
| **B** | Update `user-profile.component` for CLIENT role: | 4h |
|        | Add "My Stats" section | |
|        | Display: total spent, order count | |
|        | Display: most purchased products (simple list) | |

**Deliverable:** User dashboard shows spending analytics

---

### Day 8 (Wednesday) — Seller Dashboard Analytics

| Person | Tasks | Est. Hours |
|--------|-------|------------|
| **A** | Extend seller stats endpoint: | 3h |
|        | Best-selling products (by units) | |
|        | Revenue per product | |
| **A** | Add input validation and proper error responses | 2h |
| **B** | Update `seller-dashboard.component`: | 4h |
|        | Add "Sales Analytics" section | |
|        | Display: total revenue, total units sold | |
|        | Display: best-selling products list | |
| **B** | Add toast notifications for errors across all new components | 1h |

**Deliverable:** Seller dashboard shows sales analytics

---

### Day 9 (Thursday) — Testing & Quality

| Person | Tasks | Est. Hours |
|--------|-------|------------|
| **A** | Write Order Service tests: | 5h |
|        | Create order test | |
|        | Get orders test | |
|        | Cancel order test | |
|        | Authorization tests (user can't access other's orders) | |
| **B** | Write frontend tests: | 4h |
|        | Cart add/remove/update tests | |
|        | Order list component test | |
| **Both** | Run full Jenkins pipeline | 1h |
| **Both** | Fix any SonarQube code quality issues | 2h |

**Deliverable:** Tests passing, SonarQube gate green

---

### Day 10 (Friday) — Final Polish & Documentation

| Person | Tasks | Est. Hours |
|--------|-------|------------|
| **A** | Final API cleanup and verification via Postman | 2h |
| **A** | Verify all error codes (400, 401, 403, 404) are correct | 1h |
| **A** | Update README with new API endpoints | 1h |
| **B** | UI polish: loading spinners, empty states, error states | 3h |
| **B** | Responsive design check on all new pages | 1h |
| **Both** | Final PR review and merge | 1h |
| **Both** | Demo preparation, verify docker-compose up works | 1h |

**Deliverable:** Sprint complete, all features working, ready for demo

---

## Daily Summary Table

| Day | Person A (Backend) | Person B (Frontend) |
|-----|-------------------|---------------------|
| 1 | Order service scaffold + **DB models** | Cart page UI + **order.model.ts** |
| 2 | Order CRUD APIs | Cart functionality |
| 3 | Order statuses + cancel | Checkout wizard |
| 4 | Seller orders endpoint | Order list & detail pages |
| 5 | Search API | Seller order view + PR |
| 6 | Analytics endpoints | Search UI on products |
| 7 | User stats aggregation | User dashboard stats |
| 8 | Seller stats aggregation | Seller dashboard stats |
| 9 | Backend tests | Frontend tests |
| 10 | API cleanup + docs | UI polish + demo |

---

## Out of Scope ❌

These are explicitly NOT part of this sprint:

- Wishlist feature (bonus)
- Payment gateway integration
- Complex charts (simple numbers/lists only)
- Category management
- Admin role
- Email notifications
- Advanced pagination
- Elasticsearch (MongoDB search is sufficient)

---

## New Files to Create

### Backend (Order Service)

```
backend/services/order/
├── Dockerfile
├── pom.xml
└── src/main/java/com/buyapp/orderservice/
    ├── OrderServiceApplication.java
    ├── config/
    │   ├── SecurityConfig.java
    │   └── WebClientConfig.java
    ├── controller/
    │   └── OrderController.java
    ├── model/
    │   ├── Order.java
    │   ├── OrderItem.java
    │   └── OrderStatus.java
    ├── repository/
    │   └── OrderRepository.java
    └── service/
        └── OrderService.java
```

### Frontend (New Components)

```
frontend/src/app/
├── components/
│   ├── cart/
│   │   ├── cart.component.ts
│   │   ├── cart.component.html
│   │   └── cart.component.scss
│   ├── checkout/
│   │   ├── checkout.component.ts
│   │   ├── checkout.component.html
│   │   └── checkout.component.scss
│   └── orders/
│       ├── order-list.component.ts
│       ├── order-list.component.html
│       ├── order-list.component.scss
│       ├── order-detail.component.ts
│       ├── order-detail.component.html
│       └── order-detail.component.scss
├── models/
│   └── order.model.ts
└── services/
    └── order.service.ts
```

---

## Order Model Schema

```typescript
// Frontend: order.model.ts
interface Order {
  id?: string;
  userId: string;
  items: OrderItem[];
  status: 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
  totalAmount: number;
  shippingAddress: ShippingAddress;
  createdAt: Date;
}

interface OrderItem {
  productId: string;
  productName: string;
  sellerId: string;
  price: number;
  quantity: number;
}

interface ShippingAddress {
  fullName: string;
  address: string;
  city: string;
  phone: string;
}
```

```java
// Backend: Order.java
@Document(collection = "orders")
public class Order {
    @Id
    private String id;
    private String userId;
    private List<OrderItem> items;
    private OrderStatus status;
    private Double totalAmount;
    private ShippingAddress shippingAddress;
    private LocalDateTime createdAt;
}
```

---

## API Endpoints (New)

### Order Service

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/orders` | User | Create order from cart |
| GET | `/orders` | User | Get user's orders |
| GET | `/orders/{id}` | User | Get order details (own only) |
| PUT | `/orders/{id}/cancel` | User | Cancel order (own only) |
| GET | `/orders/seller` | Seller | Get orders with seller's products |
| GET | `/orders/stats/user` | User | Get user spending stats |
| GET | `/orders/stats/seller` | Seller | Get seller revenue stats |

### Product Service (Updated)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/products/search?q=&minPrice=&maxPrice=&sort=` | Public | Search products |

---

## Git Workflow

1. Create feature branch: `git checkout -b feature/cart-orders`
2. Daily commits with clear messages
3. Push and create PR for review
4. Both team members review each other's code
5. Jenkins must pass (tests + SonarQube)
6. Merge to `main` after approval
7. Protect `main` branch (require PR + green CI)

---

## Definition of Done ✓

A feature is complete when:

- [ ] Code is written and committed
- [ ] Code review by teammate
- [ ] Unit tests written and passing
- [ ] SonarQube quality gate passes
- [ ] Works in docker-compose environment
- [ ] Error handling implemented
- [ ] UI is responsive (if frontend)

---

## Contacts

| Role | Name | Focus Area |
|------|------|------------|
| Person A | _________ | Backend |
| Person B | _________ | Frontend |

---

## Notes

- Keep it simple — this is a school project
- Don't over-engineer solutions
- Ask questions early if blocked
- Daily sync (15 min max) recommended
- Use existing patterns from the codebase

---

*Document created: January 2026*

