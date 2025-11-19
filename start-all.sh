#!/bin/bash
# Start all Buy-01 microservices

echo "Starting Buy-01 Microservices..."
echo ""

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Check if MongoDB is running
if ! docker ps | grep -q mongodb; then
    echo -e "${YELLOW}Starting MongoDB...${NC}"
    docker run -d -p 27017:27017 --name mongodb mongo
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ Failed to start MongoDB. Checking if container exists...${NC}"
        docker start mongodb 2>/dev/null || {
            echo -e "${RED}❌ Could not start MongoDB. Please check Docker.${NC}"
            exit 1
        }
    fi
    sleep 5
fi

cd backend || exit 1

# Start each service in background
echo -e "${YELLOW}🔍 Starting Eureka server...${NC}"
cd services/eureka
nohup ../../mvnw spring-boot:run > ../../../logs/eureka.log 2>&1 &
EUREKA_PID=$!
echo -e "${GREEN}✅ Eureka started (PID: $EUREKA_PID)${NC}"
sleep 10
cd ../..

echo -e "${YELLOW} Starting User Service...${NC}"
cd services/user
nohup ../../mvnw spring-boot:run > ../../../logs/user-service.log 2>&1 &
USER_PID=$!
echo -e "${GREEN}✅ User Service started (PID: $USER_PID)${NC}"
sleep 5
cd ../..

echo -e "${YELLOW} Starting Product Service...${NC}"
cd services/product
nohup ../../mvnw spring-boot:run > ../../../logs/product-service.log 2>&1 &
PRODUCT_PID=$!
echo -e "${GREEN}✅ Product Service started (PID: $PRODUCT_PID)${NC}"
sleep 5
cd ../..

echo -e "${YELLOW}  Starting Media Service...${NC}"
cd services/media
nohup ../../mvnw spring-boot:run > ../../../logs/media-service.log 2>&1 &
MEDIA_PID=$!
echo -e "${GREEN}✅ Media Service started (PID: $MEDIA_PID)${NC}"
sleep 5
cd ../..

echo -e "${YELLOW} Starting API Gateway...${NC}"
cd api-gateway
nohup ../mvnw spring-boot:run > ../../logs/gateway.log 2>&1 &
GATEWAY_PID=$!
echo -e "${GREEN}✅ API Gateway started (PID: $GATEWAY_PID)${NC}"
cd ..

echo ""
echo -e "${GREEN} All services started!${NC}"
echo ""
echo "Services:"
echo "  Eureka:       http://localhost:8761 (PID: $EUREKA_PID)"
echo "  User Service: http://localhost:8081 (PID: $USER_PID)"
echo "  Product:      http://localhost:8082 (PID: $PRODUCT_PID)"
echo "  Media:        http://localhost:8083 (PID: $MEDIA_PID)"
echo "  Gateway:      http://localhost:8080 (PID: $GATEWAY_PID)"
echo ""
echo "Logs are in: logs/ directory"
echo "To stop all services: ./stop-all.sh"

# Save PIDs to file for stop script
mkdir -p ../logs
echo "$EUREKA_PID $USER_PID $PRODUCT_PID $MEDIA_PID $GATEWAY_PID" > ../.service-pids