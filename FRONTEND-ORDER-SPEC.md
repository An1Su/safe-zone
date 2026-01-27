# Frontend Order Implementation Specification

**For:** Frontend Developer (Person B)  
**Backend Developer:** Person A  
**Status:** Ready to implement after backend APIs are ready

---

## 📋 Overview

This document provides detailed specifications for implementing order-related frontend components. All components should follow existing patterns from `seller-dashboard`, `product-list`, and `user-profile` components.

---

## 🎯 Components to Build

### 1. **Cart Component** (`cart.component.ts/html/scss`)
**Location:** `frontend/src/app/components/cart/`

**Purpose:** Display user's cart items with quantity controls and checkout button

**Reference Pattern:** Similar to `product-list.component.ts` (list display) + `seller-dashboard.component.ts` (form handling)

**Features:**
- Display cart items from `CartService.getCart()`
- Show: product name, price, quantity, subtotal, availability status
- Quantity controls: +/- buttons (similar to seller dashboard product editing)
- Remove item button
- "Proceed with Order" button with smart visibility/state logic
- Show total amount

**Button Logic:**
```typescript
// Button visibility
const hasItems = cart.items.length > 0;
const hasAvailableItems = cart.items.some(item => item.available === true);
const buttonVisible = hasItems && hasAvailableItems;

// Button disabled state
const allItemsAvailable = cart.items.every(item => item.available === true);
const buttonDisabled = !allItemsAvailable;

// Button active
const buttonActive = allItemsAvailable;
```

**API Calls:**
- `cartService.getCart()` - Load cart
- `cartService.updateItemQuantity(productId, quantity)` - Update quantity
- `cartService.removeItem(productId)` - Remove item

**UI States:**
- Loading: Show spinner while fetching cart
- Empty: Show "Your cart is empty" message
- Items with availability issues: Show warning badges, disable checkout button
- All items available: Enable checkout button

---

### 2. **Checkout Component** (`checkout.component.ts/html/scss`)
**Location:** `frontend/src/app/components/checkout/`

**Purpose:** 2-step checkout process (cart review → shipping address → order creation)

**Reference Pattern:** Similar to `seller-dashboard.component.ts` (2-step form pattern)

**Step 1: Cart Review + Shipping Address Form**
- Display cart items (read-only)
- Show total amount
- Shipping address form fields:
  - Full Name (required, text input)
  - Address (required, textarea)
  - City (required, text input)
  - Phone (required, text input)
- Message: "Please provide delivery address"
- "Proceed with Delivery" button (disabled until form valid)

**Step 2: Order Confirmation**
- Show order created successfully
- Display order ID
- Link to order details
- Link back to products

**Form Validation:**
```typescript
// Enable "Proceed with Delivery" button when:
const formValid = 
  shippingAddress.fullName.trim() !== '' &&
  shippingAddress.address.trim() !== '' &&
  shippingAddress.city.trim() !== '' &&
  shippingAddress.phone.trim() !== '';
```

**API Calls:**
- `orderService.createOrder(shippingAddress)` - Create order from cart
- On success: Redirect to order detail page

**Error Handling:**
- Show error messages if order creation fails
- Handle stock availability errors
- Show validation errors for form fields

---

### 3. **Order List Component (Buyer)** (`order-list.component.ts/html/scss`)
**Location:** `frontend/src/app/components/orders/`

**Purpose:** Display buyer's order history with search/filter

**Reference Pattern:** Similar to `product-list.component.ts` (list + search)

**Display:**
- Order cards showing:
  - Order ID (truncated or full)
  - Order date
  - Status badge (color-coded: PENDING=yellow, READY_FOR_DELIVERY=blue, SHIPPED=purple, DELIVERED=green, CANCELLED=red)
  - Total amount
  - Number of items
  - Link to order details

**Search/Filter:**
- Search bar: Search by order ID or product name
- Filter dropdown: Filter by status (All, PENDING, READY_FOR_DELIVERY, etc.)
- Date range filter (optional): From date, To date
- Sort: By date (newest first), by total amount

**Actions:**
- Click order card → Navigate to order details
- Quick actions on card: Cancel (if PENDING/READY_FOR_DELIVERY)

**API Calls:**
- `orderService.getOrders()` - Get buyer's orders
- `orderService.searchOrders(searchParams)` - Search orders
- `orderService.cancelOrder(orderId)` - Cancel order

**Empty State:**
- Show "No orders yet" message
- Link to products page

---

### 4. **Order Detail Component (Buyer)** (`order-detail.component.ts/html/scss`)
**Location:** `frontend/src/app/components/orders/`

**Purpose:** Show detailed order information for buyer

**Reference Pattern:** Similar to `product-detail.component.ts` (detail view)

**Display:**
- Order header:
  - Order ID
  - Order date
  - Status badge
  - Total amount
- Order items list:
  - Product name, quantity, price, subtotal
  - Product image (if available)
- Shipping address:
  - Full name, address, city, phone
- Actions:
  - Cancel button (if PENDING or READY_FOR_DELIVERY)
  - Delete button (if CANCELLED or DELIVERED)
  - Redo button (always visible)

**API Calls:**
- `orderService.getOrderById(orderId)` - Get order details
- `orderService.cancelOrder(orderId)` - Cancel order
- `orderService.deleteOrder(orderId)` - Delete order
- `orderService.redoOrder(orderId)` - Redo order

**Status Colors:**
- PENDING: Yellow/Orange badge
- READY_FOR_DELIVERY: Blue badge
- SHIPPED: Purple badge
- DELIVERED: Green badge
- CANCELLED: Red/Gray badge

---

### 5. **Seller Orders Component** (`seller-orders.component.ts/html/scss`)
**Location:** `frontend/src/app/components/seller/`

**Purpose:** Display orders containing seller's products

**Reference Pattern:** Similar to `seller-dashboard.component.ts` (seller-specific view)

**Display:**
- Order cards showing:
  - Order ID
  - Buyer email/name
  - Order date
  - Status badge
  - Items (only seller's products)
  - Total for seller's items
  - Link to order details

**Search/Filter:**
- Search by: Order ID, buyer email, product name
- Filter by: Status, date range
- Filter by: Product (show only orders with specific product)

**Actions:**
- Click order → Navigate to seller order details
- Quick status update buttons on card (if PENDING → mark READY_FOR_DELIVERY)

**API Calls:**
- `orderService.getSellerOrders()` - Get seller's orders
- `orderService.searchSellerOrders(searchParams)` - Search seller orders
- `orderService.updateOrderStatus(orderId, status)` - Update status

---

### 6. **Seller Order Detail Component** (`seller-order-detail.component.ts/html/scss`)
**Location:** `frontend/src/app/components/seller/`

**Purpose:** Show order details for seller (only seller's items)

**Display:**
- Order header:
  - Order ID
  - Buyer information (email, name)
  - Order date
  - Status badge
- Order items (only seller's products):
  - Product name, quantity, price, subtotal
  - Product image
- Shipping address:
  - Full name, address, city, phone
- Status update buttons:
  - PENDING → "Mark as Ready for Delivery"
  - READY_FOR_DELIVERY → "Mark as Shipped"
  - SHIPPED → "Mark as Delivered"

**API Calls:**
- `orderService.getSellerOrderById(orderId)` - Get seller's order details
- `orderService.updateOrderStatus(orderId, newStatus)` - Update status

---

## 🔌 Order Service (`order.service.ts`)

**Location:** `frontend/src/app/services/order.service.ts`

**Reference Pattern:** Follow `product.service.ts` structure

**Methods:**
```typescript
// Buyer endpoints
createOrder(shippingAddress: ShippingAddress): Observable<Order>
getOrders(): Observable<Order[]>
getOrderById(orderId: string): Observable<Order>
cancelOrder(orderId: string): Observable<void>
deleteOrder(orderId: string): Observable<void>
redoOrder(orderId: string): Observable<Order>
searchOrders(params: SearchParams): Observable<Order[]>

// Seller endpoints
getSellerOrders(): Observable<Order[]>
getSellerOrderById(orderId: string): Observable<Order>
updateOrderStatus(orderId: string, status: OrderStatus): Observable<Order>
searchSellerOrders(params: SearchParams): Observable<Order[]>

// Stats endpoints
getUserStats(): Observable<UserStats>
getSellerStats(): Observable<SellerStats>
```

**API Base URL:** `${environment.apiUrl}/orders`

**Authentication:** Use `authService.getAuthHeaders()` for authenticated requests

---

## 📝 Order Models (`order.model.ts`)

**Location:** `frontend/src/app/models/order.model.ts`

**Reference Pattern:** Similar to `ecommerce.model.ts` (Product, User interfaces)

**Interfaces:**
```typescript
export interface Order {
  id?: string;
  userId: string;
  items: OrderItem[];
  status: OrderStatus;
  totalAmount: number;
  shippingAddress: ShippingAddress;
  createdAt: Date;
  updatedAt?: Date;
}

export interface OrderItem {
  productId: string;
  productName: string;
  sellerId: string;
  quantity: number;
  price: number;
}

export interface ShippingAddress {
  fullName: string;
  address: string;
  city: string;
  phone: string;
}

export type OrderStatus = 
  | 'PENDING' 
  | 'READY_FOR_DELIVERY' 
  | 'SHIPPED' 
  | 'DELIVERED' 
  | 'CANCELLED';

export interface OrderSearchParams {
  q?: string;           // Search query (order ID, product name, buyer email)
  status?: OrderStatus; // Filter by status
  dateFrom?: string;    // Filter from date
  dateTo?: string;      // Filter to date
  sort?: 'date' | 'amount'; // Sort by
}
```

---

## 🛣️ Routes (`app.routes.ts`)

**Add these routes:**

```typescript
// Buyer routes (require auth)
{
  path: 'cart',
  component: CartComponent,
  canActivate: [authGuard],
},
{
  path: 'checkout',
  component: CheckoutComponent,
  canActivate: [authGuard],
},
{
  path: 'orders',
  component: OrderListComponent,
  canActivate: [authGuard],
},
{
  path: 'orders/:id',
  component: OrderDetailComponent,
  canActivate: [authGuard],
},

// Seller routes (require seller role)
{
  path: 'seller/orders',
  component: SellerOrdersComponent,
  canActivate: [sellerGuard],
},
{
  path: 'seller/orders/:id',
  component: SellerOrderDetailComponent,
  canActivate: [sellerGuard],
},
```

---

## 🎨 UI/UX Patterns

### Status Badges
**Reference:** Use similar badge styling from seller dashboard

```scss
.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 0.85rem;
  font-weight: 600;
  
  &.pending { background: #fef3c7; color: #92400e; }
  &.ready-for-delivery { background: #dbeafe; color: #1e40af; }
  &.shipped { background: #e9d5ff; color: #6b21a8; }
  &.delivered { background: #d1fae5; color: #065f46; }
  &.cancelled { background: #f3f4f6; color: #374151; }
}
```

### Button States
**Reference:** Follow button patterns from `home.component.html` (btn-hero-primary, btn-hero-secondary)

```html
<!-- Visible but disabled -->
<button 
  class="btn-primary" 
  [disabled]="!allItemsAvailable"
  [class.disabled]="!allItemsAvailable">
  Proceed with Order
</button>

<!-- Hidden -->
<button 
  *ngIf="hasItems && hasAvailableItems"
  class="btn-primary">
  Proceed with Order
</button>
```

### Loading States
**Reference:** Follow loading pattern from `home.component.html`

```html
<div *ngIf="loading" class="loading">
  <div class="loading-spinner"></div>
  <span>Loading orders...</span>
</div>
```

### Empty States
**Reference:** Follow empty state pattern from `home.component.html`

```html
<div *ngIf="orders.length === 0 && !loading" class="empty-state">
  <svg>...</svg>
  <h3>No orders yet</h3>
  <p>Start shopping to see your orders here</p>
  <a routerLink="/products" class="btn-primary">Browse Products</a>
</div>
```

---

## 🔄 Component Flow

### Cart → Checkout → Order Flow

```
1. User adds items to cart (via product pages)
   ↓
2. User navigates to /cart
   ↓
3. Cart component loads cart items
   ↓
4. If all items available → "Proceed with Order" button active
   ↓
5. User clicks "Proceed with Order"
   ↓
6. Navigate to /checkout
   ↓
7. Checkout component shows:
   - Cart review (read-only)
   - Shipping address form
   ↓
8. User fills shipping address
   ↓
9. "Proceed with Delivery" button becomes active
   ↓
10. User clicks "Proceed with Delivery"
    ↓
11. POST /orders API call
    ↓
12. On success:
    - Show success message
    - Redirect to /orders/:id (order detail)
    - Cart is cleared (backend handles this)
```

### Order Management Flow

**Buyer:**
```
/orders → Order list
  ↓
Click order → /orders/:id → Order detail
  ↓
Actions: Cancel, Delete, Redo
```

**Seller:**
```
/seller/orders → Seller order list
  ↓
Click order → /seller/orders/:id → Seller order detail
  ↓
Actions: Update status (PENDING → READY_FOR_DELIVERY → SHIPPED → DELIVERED)
```

---

## 📱 Responsive Design

**Reference:** Follow responsive patterns from `home.component.scss`

- Mobile: Single column layout, full-width buttons
- Tablet: 2-column grid for order cards
- Desktop: 3-column grid for order cards

**Breakpoints:** Use existing SCSS variables (`$breakpoint-mobile`, `$breakpoint-tablet`, `$breakpoint-desktop`)

---

## ✅ Testing Checklist

**Cart Component:**
- [ ] Loads cart items correctly
- [ ] Shows availability status
- [ ] Button visibility logic works
- [ ] Quantity controls work
- [ ] Remove item works
- [ ] Empty state displays correctly

**Checkout Component:**
- [ ] Form validation works
- [ ] Button enables when form valid
- [ ] Order creation works
- [ ] Error handling works
- [ ] Success redirect works

**Order List (Buyer):**
- [ ] Loads orders correctly
- [ ] Search works
- [ ] Filter works
- [ ] Status badges display correctly
- [ ] Navigation to detail works

**Order Detail (Buyer):**
- [ ] Displays order details correctly
- [ ] Cancel button works (if allowed)
- [ ] Delete button works (if allowed)
- [ ] Redo button works

**Seller Orders:**
- [ ] Loads seller orders correctly
- [ ] Shows only seller's items
- [ ] Status update works
- [ ] Search/filter works

---

## 🔗 Integration Points

**Cart Service Integration:**
- Use existing `CartService` from `cart.service.ts`
- Cart component calls: `getCart()`, `updateItemQuantity()`, `removeItem()`

**Order Service Integration:**
- Create new `OrderService` following `ProductService` pattern
- All order components use `OrderService`

**Auth Integration:**
- Use `AuthService` to check user role
- Buyer views: Check `user.role === 'client'`
- Seller views: Use `sellerGuard` for routes

**Navigation:**
- Add links in navbar to `/cart` and `/orders`
- Seller navbar: Add link to `/seller/orders`

---

## 📚 Reference Components

**For Cart Component:**
- `product-list.component.ts` - List display pattern
- `seller-dashboard.component.ts` - Form handling pattern

**For Checkout Component:**
- `seller-dashboard.component.ts` - Multi-step form pattern
- `login.component.ts` - Form validation pattern

**For Order List:**
- `product-list.component.ts` - List + search pattern
- `seller-dashboard.component.ts` - Seller-specific view pattern

**For Order Detail:**
- `product-detail.component.ts` - Detail view pattern
- `user-profile.component.ts` - Action buttons pattern

---

## 🎯 Key Differences: Buyer vs Seller Views

| Feature | Buyer View | Seller View |
|---------|-----------|-------------|
| **Orders Shown** | Own orders only | Orders with seller's products |
| **Items Displayed** | All items in order | Only seller's items |
| **Actions** | Cancel, Delete, Redo | Update status only |
| **Search** | By order ID, product name | By order ID, buyer email, product |
| **Status Updates** | Cannot update | Can update (PENDING → READY_FOR_DELIVERY → etc.) |
| **Route** | `/orders` | `/seller/orders` |
| **Guard** | `authGuard` | `sellerGuard` |

---

## 🚨 Important Notes

1. **Stock Reduction:** Backend handles stock reduction automatically when order is created
2. **Cart Clearing:** Backend clears cart automatically when order is created
3. **Status Updates:** Only sellers can update order status
4. **Order Cancellation:** Only buyers can cancel, and only if PENDING or READY_FOR_DELIVERY
5. **Stock Restoration:** Backend restores stock when order is cancelled
6. **Error Handling:** Always show user-friendly error messages
7. **Loading States:** Show spinners during API calls
8. **Empty States:** Provide helpful messages and links

---

## 📞 Backend API Contract

**Wait for backend to provide:**
- Order Service endpoints (see API Endpoints section above)
- Order DTO structure
- Error response format
- Authentication headers format

**Once backend is ready:**
- Test each endpoint with Postman/curl
- Verify response formats match TypeScript interfaces
- Test error scenarios (404, 400, 403, etc.)

---

**Document Version:** 1.0  
**Last Updated:** January 2026  
**Status:** Ready for implementation after backend APIs are complete
