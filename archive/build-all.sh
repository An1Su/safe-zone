#!/bin/bash
# filepath: build-all.sh

echo "Building E-com Microservices..."
echo ""

# Color codes for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✅ $2${NC}"
    else
        echo -e "${RED}❌ $2 failed${NC}"
        exit 1
    fi
}

# NAvigate to backend directory
cd backend || exit 1

# 1. Build shared module first (Critical!)
echo -e "${YELLOW} Building shared module...${NC}"
cd shared
../mvnw clean install -DskipTests
print_status $? "Shared module"
cd ..

# 2. Eureka server
echo -e "${YELLO} Building Eureka server...${NC}"
cd services/eureka
../../mvnw clean install -DskipTests
print_status $? "Eureka server"
cd ../..

# 3. User Service
echo -e "${YELLO} Building User Service...${NC}"
cd services/user
../../mvnw clean install -DskipTests
print_status $? "User Service"
cd ../..

# 4. Product Service
echo -e "${YELLO} Building Product Service...${NC}"
cd services/product
../../mvnw clean install -DskipTests
print_status $? "Product Service"
cd ../..

# 5. Media Service
echo -e "${YELLO} Building Media Service...${NC}"
cd services/media
../../mvnw clean install -DskipTests
print_status $? "Media Service"
cd ../..

# 6. API Gateway
echo -e "${YELLO} Building API Gateway...${NC}"
cd api-gateway
../mvnw clean install -DskipTests
print_status $? "API Gateway"
cd ../..

echo ""
echo -e "${GREEN} All services built successfully!${NC}"
echo ""
echo "Next steps:"
echo "  1. Start MongoDB: docker run -d -p 27017:27017 --name mongodb mongo"
echo "  2. Run services: ./start-all.sh"
echo "  3. Or use Docker: docker-compose up"