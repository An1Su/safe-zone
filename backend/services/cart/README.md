# Cart Service

Cart management microservice for the e-commerce platform.

## Features

- **Cart Management**: Add, update, remove items from cart
- **Persistence**: Cart data stored in MongoDB
- **Product Integration**: Validates products and stock via Product Service
- **User-specific**: Each user has their own cart

## API Endpoints

- `GET /cart` - Get user's cart
- `POST /cart/items` - Add item to cart
- `PUT /cart/items/{productId}` - Update item quantity
- `DELETE /cart/items/{productId}` - Remove item from cart
- `DELETE /cart` - Clear entire cart

## Database

- **Database**: `cart_service_db`
- **Collection**: `carts`
- **Port**: `8084`

## Service Discovery

- Registered with Eureka as `cart-service`

