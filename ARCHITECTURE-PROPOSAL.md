# Architecture Simplification Proposal: Combine Cart & Order Services

## Current Architecture Analysis

### Current State
- **Cart Service**: Separate microservice (port 8084)
  - Own MongoDB collection: `carts`
  - Own database: `cart_service_db`
  - Cart CRUD operations
  - Stock validation via Product Service
  
- **Order Service**: Separate microservice (port 8085)
  - Own MongoDB collection: `orders`
  - Own database: `order_service_db`
  - Calls Cart Service via WebClient to get cart
  - Creates order from cart, then clears cart via WebClient

### Problems with Current Architecture
1. **Tight Coupling**: Order Service depends heavily on Cart Service
2. **Network Overhead**: Every order creation requires 2 HTTP calls (get cart + clear cart)
3. **Complexity**: Two microservices for closely related functionality
4. **Transaction Risk**: Cart clearing can fail independently of order creation
5. **Over-engineering**: For a study project, this separation adds unnecessary complexity

## Proposed Solution: Unified Order Service

### New Architecture
- **Order Service**: Single microservice containing both cart and order functionality
  - MongoDB collections:
    - `carts` (separate collection as requested)
    - `orders`
  - Single database: `order_service_db`
  - Cart operations use local repository (no WebClient calls)
  - Order operations use local cart repository

### Benefits
1. ✅ **Simpler**: One service instead of two
2. ✅ **Faster**: No inter-service HTTP calls for cart operations
3. ✅ **Atomic**: Cart clearing happens in same transaction context
4. ✅ **Easier to maintain**: All cart/order logic in one place
5. ✅ **Study-friendly**: More appropriate complexity level for learning

### Implementation Plan

#### Step 1: Move Cart Models to Order Service
- Copy `Cart.java` and `CartItem.java` to `order-service/model/`
- Keep them as separate models (Cart is not Order)

#### Step 2: Move Cart Repository to Order Service
- Copy `CartRepository.java` to `order-service/repository/`
- Update MongoDB collection name to `carts` (same as before)

#### Step 3: Integrate CartService into OrderService
- Add cart methods to `OrderService`:
  - `getCart(userId)` - Get or create cart
  - `addItem(userId, item)` - Add item to cart
  - `updateItemQuantity(userId, productId, quantity)` - Update quantity
  - `removeItem(userId, productId)` - Remove item
  - `clearCart(userId)` - Clear cart (used internally)
- Remove WebClient calls to Cart Service
- Use local `CartRepository` instead

#### Step 4: Update OrderController
- Add cart endpoints to `OrderController`:
  - `GET /orders/cart` - Get cart
  - `POST /orders/cart/items` - Add item
  - `PUT /orders/cart/items/{productId}` - Update quantity
  - `DELETE /orders/cart/items/{productId}` - Remove item
- Keep existing order endpoints unchanged

#### Step 5: Update API Gateway
- Remove Cart Service route
- Update cart routes to point to Order Service:
  ```yaml
  - id: cart-service
    uri: lb://order-service
    predicates:
      - Path=/cart/**
  ```

#### Step 6: Cleanup
- Delete `backend/services/cart/` directory
- Remove Cart Service from `backend/pom.xml` modules
- Remove Cart Service from `docker-compose.yml`
- Update `build-all.sh` and `start-all.sh` scripts

### Database Structure

```
order_service_db
├── carts (collection)
│   ├── _id
│   ├── userId
│   ├── items[]
│   ├── createdAt
│   └── updatedAt
│
└── orders (collection)
    ├── _id
    ├── userId
    ├── items[]
    ├── status
    ├── shippingAddress
    ├── totalAmount
    ├── createdAt
    └── updatedAt
```

### API Endpoints (After Migration)

**Cart Endpoints** (now in Order Service):
- `GET /cart` - Get user's cart
- `POST /cart/items` - Add item to cart
- `PUT /cart/items/{productId}` - Update item quantity
- `DELETE /cart/items/{productId}` - Remove item from cart

**Order Endpoints** (existing):
- `POST /orders` - Create order from cart
- `GET /orders` - Get buyer's orders
- `GET /orders/{id}` - Get order by ID
- `PUT /orders/{id}/cancel` - Cancel order
- `DELETE /orders/{id}` - Delete order
- `POST /orders/{id}/redo` - Redo cancelled order
- `GET /orders/search` - Search orders
- `GET /orders/seller` - Get seller orders
- `GET /orders/seller/{id}` - Get seller order by ID
- `PUT /orders/{id}/status` - Update order status
- `GET /orders/seller/search` - Search seller orders

### Migration Impact

#### Frontend
- **No changes needed** if API Gateway routes are updated correctly
- Cart endpoints will still work at `/cart/**` but route to Order Service

#### Other Services
- **No changes needed** - no other services call Cart Service directly

#### Docker/Deployment
- One less container to manage
- Simpler docker-compose.yml
- Faster startup time

### Code Changes Summary

| File | Action | Reason |
|------|--------|--------|
| `order-service/model/Cart.java` | Add | Move from cart-service |
| `order-service/model/CartItem.java` | Add | Move from cart-service |
| `order-service/repository/CartRepository.java` | Add | Move from cart-service |
| `order-service/service/OrderService.java` | Modify | Add cart methods, remove WebClient calls |
| `order-service/controller/OrderController.java` | Modify | Add cart endpoints |
| `api-gateway/application.yml` | Modify | Update cart route to order-service |
| `backend/pom.xml` | Modify | Remove cart-service module |
| `docker-compose.yml` | Modify | Remove cart-service container |
| `backend/services/cart/` | Delete | Entire directory |

### Testing Strategy

1. **Unit Tests**: Test cart methods in OrderService
2. **Integration Tests**: Test cart → order flow
3. **API Tests**: Verify all cart endpoints work
4. **Migration Test**: Ensure existing carts are accessible

### Rollback Plan

If issues arise:
1. Keep Cart Service code in git history
2. Revert API Gateway changes
3. Restart Cart Service container
4. No data migration needed (carts stay in same collection)

## Recommendation

✅ **Proceed with consolidation** - This simplification is appropriate for a study project and reduces unnecessary complexity while maintaining clean separation of concerns (cart vs order collections).
