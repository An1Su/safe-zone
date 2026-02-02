# Database Seed Scripts

This directory contains scripts to populate the MongoDB database with test data.

## Quick Start

### Option 1: Using Docker (Recommended)

If your MongoDB is running in Docker:

```bash
# Copy the seed script into the MongoDB container
docker cp backend/scripts/seed-data.js ecom-mongodb:/tmp/seed-data.js

# Execute the script inside the container
docker exec -i ecom-mongodb mongosh "mongodb://admin:password@localhost:27017/admin?authSource=admin" < backend/scripts/seed-data.js
```

Or run it directly:

```bash
docker exec -i ecom-mongodb mongosh "mongodb://admin:password@localhost:27017/admin?authSource=admin" --file /tmp/seed-data.js
```

### Option 2: Using mongosh directly

If you have mongosh installed locally:

```bash
mongosh "mongodb://admin:password@localhost:27017/admin?authSource=admin" --file backend/scripts/seed-data.js
```

### Option 3: Using the shell script

```bash
chmod +x backend/scripts/seed-data.sh
./backend/scripts/seed-data.sh
```

## What Gets Created

### Users (4 total)
- **seller1@gmail.com** (password: 12345) - Role: seller
- **seller2@gmail.com** (password: 12345) - Role: seller
- **buyer1@gmail.com** (password: 12345) - Role: client
- **buyer2@gmail.com** (password: 12345) - Role: client

### Products (16 total)
- **Seller 1** has 8 products (IDs 1-8 from your mock data)
- **Seller 2** has 8 products (IDs 9-16 from your mock data)

## Notes

- The script uses BCrypt hashed passwords (pre-computed hash for "12345")
- All users and products are created in the `ecommerce` database
- The script will delete existing users and products before creating new ones
- To keep existing data, comment out the `deleteMany` lines in the script

## Troubleshooting

If you get authentication errors:
1. Make sure MongoDB is running
2. Verify the connection string matches your setup
3. Check that the admin user credentials are correct (admin/password)

If you get "database not found" errors:
- The script creates the `ecommerce` database automatically when inserting data
- Make sure your MongoDB connection allows database creation
