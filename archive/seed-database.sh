#!/bin/bash

# Database Seeding Script
# This script seeds MongoDB with test users and products

echo "🌱 Seeding MongoDB database..."

# Check if MongoDB container is running
if ! docker ps | grep -q ecom-mongodb; then
    echo "❌ Error: MongoDB container (ecom-mongodb) is not running"
    echo "Please start it with: docker-compose up -d mongodb"
    exit 1
fi

# Create seed script content
cat > /tmp/seed-data-temp.js << 'EOF'
// MongoDB Seed Data Script
use ecommerce;

// Clear existing data
print("Clearing existing data...");
db.users.deleteMany({});
db.products.deleteMany({});

// BCrypt hash for password "12345"
const passwordHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

// Create Users
print("Creating users...");

const seller1 = db.users.insertOne({
  name: "Seller One",
  email: "seller1@gmail.com",
  password: passwordHash,
  role: "seller",
  avatar: null
});

const seller2 = db.users.insertOne({
  name: "Seller Two",
  email: "seller2@gmail.com",
  password: passwordHash,
  role: "seller",
  avatar: null
});

const buyer1 = db.users.insertOne({
  name: "Buyer One",
  email: "buyer1@gmail.com",
  password: passwordHash,
  role: "client",
  avatar: null
});

const buyer2 = db.users.insertOne({
  name: "Buyer Two",
  email: "buyer2@gmail.com",
  password: passwordHash,
  role: "client",
  avatar: null
});

// Get seller IDs
const seller1Id = seller1.insertedId.toString();
const seller2Id = seller2.insertedId.toString();

print("✅ Users created!");
print("   Seller 1 ID: " + seller1Id);
print("   Seller 2 ID: " + seller2Id);

// Create Products for Seller 1
print("Creating products for seller1...");

db.products.insertMany([
  {
    name: "Velvet Matte Lipstick",
    description: "Long-lasting matte lipstick with velvety finish. Rich color payoff that stays put all day.",
    price: 24.99,
    stock: 50,
    userId: seller1Id
  },
  {
    name: "Rose Gold Eyeshadow Palette",
    description: "12-shade eyeshadow palette featuring rose gold tones. Highly pigmented and blendable.",
    price: 48.99,
    stock: 30,
    userId: seller1Id
  },
  {
    name: "Volumizing Drama Mascara",
    description: "Builds volume and length for dramatic lashes. Waterproof formula.",
    price: 19.99,
    stock: 75,
    userId: seller1Id
  },
  {
    name: "Silk Glow Blush",
    description: "Silky smooth blush with natural glow finish. Buildable coverage.",
    price: 28.99,
    stock: 40,
    userId: seller1Id
  },
  {
    name: "Crystal Shine Lip Gloss",
    description: "High-shine lip gloss with crystal shimmer. Non-sticky formula.",
    price: 16.99,
    stock: 60,
    userId: seller1Id
  },
  {
    name: "Diamond Dust Highlighter",
    description: "Luminous highlighter with diamond-like shimmer. Creates a dewy glow.",
    price: 34.99,
    stock: 35,
    userId: seller1Id
  },
  {
    name: "Berry Bliss Lipstick",
    description: "Rich berry-toned lipstick with satin finish. Comfortable wear.",
    price: 22.99,
    stock: 45,
    userId: seller1Id
  },
  {
    name: "Smoky Night Palette",
    description: "10-shade palette perfect for creating smoky eye looks. Includes matte and shimmer.",
    price: 52.99,
    stock: 25,
    userId: seller1Id
  }
]);

// Create Products for Seller 2
print("Creating products for seller2...");

db.products.insertMany([
  {
    name: "Lengthening Lash Mascara",
    description: "Lengthens and separates lashes for a natural look. Smudge-proof.",
    price: 18.99,
    stock: 55,
    userId: seller2Id
  },
  {
    name: "Coral Dream Blush",
    description: "Vibrant coral blush with soft matte finish. Perfect for warm skin tones.",
    price: 26.99,
    stock: 42,
    userId: seller2Id
  },
  {
    name: "Nude Shimmer Gloss",
    description: "Nude-toned lip gloss with subtle shimmer. Everyday essential.",
    price: 14.99,
    stock: 70,
    userId: seller2Id
  },
  {
    name: "Golden Hour Highlighter",
    description: "Warm golden highlighter for a sun-kissed glow. Buildable intensity.",
    price: 32.99,
    stock: 38,
    userId: seller2Id
  },
  {
    name: "Ruby Red Lipstick",
    description: "Classic red lipstick with creamy formula. Bold and beautiful.",
    price: 26.99,
    stock: 48,
    userId: seller2Id
  },
  {
    name: "Peach Perfect Blush",
    description: "Soft peach blush with natural finish. Flatters all skin tones.",
    price: 24.99,
    stock: 50,
    userId: seller2Id
  },
  {
    name: "Sunset Glow Palette",
    description: "15-shade palette inspired by sunset colors. Mix of matte and metallic.",
    price: 44.99,
    stock: 28,
    userId: seller2Id
  },
  {
    name: "Waterproof Wonder Mascara",
    description: "Waterproof mascara that won't budge. Perfect for active days.",
    price: 21.99,
    stock: 65,
    userId: seller2Id
  }
]);

print("✅ Products created!");
print("\n📊 Summary:");
print("   Users created: 4 (2 sellers, 2 buyers)");
print("   Products created: 16 (8 per seller)");
print("\n🔑 Login credentials:");
print("   Email: seller1@gmail.com | Password: 12345");
print("   Email: seller2@gmail.com | Password: 12345");
print("   Email: buyer1@gmail.com | Password: 12345");
print("   Email: buyer2@gmail.com | Password: 12345");
EOF

# Copy script to container and execute
docker cp /tmp/seed-data-temp.js ecom-mongodb:/tmp/seed-data.js
docker exec -i ecom-mongodb mongosh "mongodb://admin:password@localhost:27017/admin?authSource=admin" --file /tmp/seed-data.js

# Cleanup
rm -f /tmp/seed-data-temp.js

echo ""
echo "✅ Database seeding completed!"
