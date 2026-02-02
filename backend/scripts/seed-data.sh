#!/bin/bash

# MongoDB Seed Data Script Runner
# This script connects to MongoDB and runs the seed data script

echo "Seeding MongoDB database..."

# Check if mongosh is available
if ! command -v mongosh &> /dev/null; then
    echo "Error: mongosh is not installed or not in PATH"
    echo "Please install MongoDB Shell (mongosh) or use Docker exec"
    exit 1
fi

# Run the seed script
mongosh "mongodb://admin:password@localhost:27017/admin?authSource=admin" --file backend/scripts/seed-data.js

echo "Done!"
