# Frontend Handoff: Shopping Cart & Orders
**Date:** January 27, 2026  
**Author:** Ekaterina (Frontend)  
**For:** Backend Developer (Anastasia)

---

## Summary

Today we implemented the complete shopping cart and checkout frontend flow. The UI is ready, but it needs the **Order Service backend** to be created.

---

## What Was Built

### 1. Cart Page (`/cart`)
- Display cart items with images, quantities, and prices
- Quantity controls (+/- buttons)
- Remove individual items
- Select all / Remove selected (bulk operations)
- Stock validation warnings
- Order summary sidebar with totals

### 2. Checkout Page (`/checkout`)
- Shipping address form (name, address, city, phone)
- Order review with item list
- "Pay on Delivery" payment method
- Place order button

### 3. Order Confirmation Page (`/order-confirmation/:id`)
- Thank you message
- Order ID display
- Link to order history (future feature)

### 4. Navbar Updates
- Cart icon only visible for logged-in **buyers** (not sellers, not guests)
- Cart badge shows item count

---

## API Contracts Needed

### Order Service Endpoints

The frontend expects these endpoints at `/orders`:

#### 1. Create Order
```
POST /orders
Authorization: Bearer <token>
Content-Type: application/json

Request Body:
{
  "items": [
    {
      "productId": "string",
      "productName": "string",
      "sellerId": "string",
      "price": number,
      "quantity": number
    }
  ],
  "shippingAddress": {
    "fullName": "string",
    "address": "string",
    "city": "string",
    "phone": "string"
  }
}

Response (201 Created):
{
  "id": "string",
  "userId": "string",
  "items": [...],
  "status": "PENDING",
  "totalAmount": number,
  "shippingAddress": {...},
  "createdAt": "ISO date string"
}
```

#### 2. Get User's Orders
```
GET /orders
Authorization: Bearer <token>

Response (200 OK):
[
  {
    "id": "string",
    "userId": "string",
    "items": [...],
    "status": "PENDING" | "CONFIRMED" | "SHIPPED" | "DELIVERED" | "CANCELLED",
    "totalAmount": number,
    "shippingAddress": {...},
    "createdAt": "ISO date string"
  }
]
```

#### 3. Get Order by ID
```
GET /orders/{orderId}
Authorization: Bearer <token>

Response (200 OK):
{
  "id": "string",
  "userId": "string",
  "items": [...],
  "status": "string",
  "totalAmount": number,
  "shippingAddress": {...},
  "createdAt": "ISO date string"
}
```

#### 4. Cancel Order (optional)
```
PUT /orders/{orderId}/cancel
Authorization: Bearer <token>

Response (200 OK):
{ ...order with status: "CANCELLED" }
```

#### 5. Get User Stats (optional)
```
GET /orders/stats/user
Authorization: Bearer <token>

Response (200 OK):
{
  "totalOrders": number,
  "totalSpent": number,
  "pendingOrders": number
}
```

---

## Data Models

### Frontend Models (for reference)

**File:** `frontend/src/app/models/order.model.ts`

```typescript
export interface Order {
  id?: string;
  userId: string;
  items: OrderItem[];
  status: 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
  totalAmount: number;
  shippingAddress: ShippingAddress;
  createdAt: Date;
}

export interface OrderItem {
  productId: string;
  productName: string;
  sellerId: string;
  price: number;
  quantity: number;
}

export interface ShippingAddress {
  fullName: string;
  address: string;
  city: string;
  phone: string;
}

export interface CreateOrderRequest {
  items: OrderItem[];
  shippingAddress: ShippingAddress;
}
```

**File:** `frontend/src/app/models/cart.model.ts` (updated)

```typescript
export interface CartItem {
  id?: string;
  productId: string;
  productName: string;
  price: number;
  quantity: number;
  image?: string;
  sellerId: string;  // Added for order tracking
  stock: number;     // Added for validation
}
```

---

## API Gateway Routing

The Order Service needs to be added to the API Gateway. Suggested route:

```yaml
# In API Gateway configuration
- id: order-service
  uri: lb://ORDER-SERVICE
  predicates:
    - Path=/orders/**
  filters:
    - AuthenticationFilter
```

---

## Database Schema Suggestion

### Orders Table
| Column | Type | Notes |
|--------|------|-------|
| id | UUID | Primary key |
| user_id | UUID | FK to users |
| status | ENUM | PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED |
| total_amount | DECIMAL | Sum of all items |
| shipping_full_name | VARCHAR | |
| shipping_address | VARCHAR | |
| shipping_city | VARCHAR | |
| shipping_phone | VARCHAR | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

### Order Items Table
| Column | Type | Notes |
|--------|------|-------|
| id | UUID | Primary key |
| order_id | UUID | FK to orders |
| product_id | UUID | FK to products |
| product_name | VARCHAR | Snapshot at order time |
| seller_id | UUID | FK to users |
| price | DECIMAL | Snapshot at order time |
| quantity | INT | |

---

## Business Logic Notes

1. **Stock Validation:** Before creating an order, validate that all products have sufficient stock
2. **Stock Deduction:** After order creation, reduce product stock by ordered quantities
3. **Price Snapshot:** Store the price at order time (don't reference current product price)
4. **Seller Tracking:** Each order item includes sellerId for future seller dashboard features

---

## Files Changed Today

### New Files
- `frontend/src/app/components/cart/cart.component.ts`
- `frontend/src/app/components/cart/cart.component.html`
- `frontend/src/app/components/cart/cart.component.scss`
- `frontend/src/app/components/checkout/checkout.component.ts`
- `frontend/src/app/components/checkout/checkout.component.html`
- `frontend/src/app/components/checkout/checkout.component.scss`
- `frontend/src/app/components/order-confirmation/order-confirmation.component.ts`
- `frontend/src/app/components/order-confirmation/order-confirmation.component.html`
- `frontend/src/app/components/order-confirmation/order-confirmation.component.scss`
- `frontend/src/app/models/order.model.ts`
- `frontend/src/app/services/order.service.ts`

### Modified Files
- `frontend/src/app/models/cart.model.ts` - Added sellerId, stock
- `frontend/src/app/app.routes.ts` - Added cart, checkout, order-confirmation routes
- `frontend/src/app/components/navbar/*` - Cart icon for buyers only
- `frontend/src/app/components/products/*` - Updated addToCart with sellerId, stock
- `frontend/src/app/services/cart.service.spec.ts` - Fixed test mocks

### Jenkins Changes
- `Jenkinsfile` - Deploy only for main or branches containing "frontend"

---

## Testing the Frontend

Once the Order Service is deployed:

1. Login as a buyer
2. Add products to cart
3. Go to `/cart`
4. Click "Proceed to Checkout"
5. Fill shipping form
6. Click "Place Order"
7. Should redirect to `/order-confirmation/{orderId}`

---

## Questions for Discussion

1. Should we combine Cart + Order into one service? (as discussed)
2. Do we need email notifications on order creation?
3. Should sellers be able to update order status (SHIPPED, DELIVERED)?

---

Let me know if you have any questions! 🚀

