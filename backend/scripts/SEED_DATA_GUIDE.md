# Seed Data Guide

This guide explains how the automatic seed data initialization works.

## How It Works

The application automatically seeds the database with test data when services start **if the database is empty**.

### Startup Order

1. **User Service** - Creates 4 users (2 sellers, 2 buyers)
2. **Product Service** - Waits for users, then creates 16 products (8 per seller)
3. **Media Service** - Waits for products, then creates placeholder images for each product

### Configuration

All seed data initializers are in:
- `backend/services/user/src/main/java/com/buyapp/userservice/config/DataInitializer.java`
- `backend/services/product/src/main/java/com/buyapp/productservice/config/DataInitializer.java`
- `backend/services/media/src/main/java/com/buyapp/mediaservice/config/DataInitializer.java`

They use Spring Boot's `CommandLineRunner` which executes automatically on startup.

## Test Accounts

All accounts use password: **12345**

### Sellers
- `seller1@gmail.com` - Seller One
- `seller2@gmail.com` - Seller Two

### Buyers
- `buyer1@gmail.com` - Buyer One
- `buyer2@gmail.com` - Buyer Two

## Products

### Seller 1 Products (8 items)
1. Velvet Matte Lipstick - $24.99
2. Rose Gold Eyeshadow Palette - $48.99
3. Volumizing Drama Mascara - $19.99
4. Silk Glow Blush - $28.99
5. Crystal Shine Lip Gloss - $16.99
6. Diamond Dust Highlighter - $34.99
7. Berry Bliss Lipstick - $22.99
8. Smoky Night Palette - $52.99

### Seller 2 Products (8 items)
1. Lengthening Lash Mascara - $18.99
2. Coral Dream Blush - $26.99
3. Nude Shimmer Gloss - $14.99
4. Golden Hour Highlighter - $32.99
5. Ruby Red Lipstick - $26.99
6. Peach Perfect Blush - $24.99
7. Sunset Glow Palette - $44.99
8. Waterproof Wonder Mascara - $21.99

## Images

- Each product gets a placeholder image automatically
- Placeholder images are minimal 1x1 transparent PNGs
- To add real images:
  1. Log in as a seller
  2. Go to seller dashboard
  3. Upload images for your products

## Resetting Seed Data

To reset and re-seed:

```bash
# Stop services
docker-compose down

# Remove volumes (this deletes all data)
docker-compose down -v

# Start services (will auto-seed)
docker-compose up -d
```

Or manually clear MongoDB:

```bash
docker exec -i ecom-mongodb mongosh "mongodb://admin:password@localhost:27017/admin?authSource=admin" --eval "use ecommerce; db.users.deleteMany({}); db.products.deleteMany({}); db.media.deleteMany({});"
```

Then restart services to re-seed.

## Disabling Auto-Seed

To disable automatic seeding, comment out or remove the `@Bean` methods in the DataInitializer classes, or set a property to control it.

## Troubleshooting

### Products not created
- Check that User Service started first and users were created
- Check Product Service logs for retry messages
- Ensure Eureka is running for service discovery

### Images not created
- Check that Product Service created products first
- Check Media Service logs for retry messages
- Ensure uploads directory is writable

### Services not finding each other
- Ensure Eureka Server is running
- Check service registration in Eureka dashboard: http://localhost:8761
- Verify service names match (user-service, product-service, media-service)
