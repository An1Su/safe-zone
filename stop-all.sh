#!/bin/bash
# filepath: stop-all.sh

echo "Stopping Buy-01 Microservices..."

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

# Read PIDs from file if it exists
if [ -f .service-pids ]; then
    read -r EUREKA_PID USER_PID PRODUCT_PID MEDIA_PID GATEWAY_PID < .service-pids
    
    echo "Stopping services..."
    kill $EUREKA_PID 2>/dev/null && echo -e "${GREEN}✅ Eureka stopped${NC}"
    kill $USER_PID 2>/dev/null && echo -e "${GREEN}✅ User Service stopped${NC}"
    kill $PRODUCT_PID 2>/dev/null && echo -e "${GREEN}✅ Product Service stopped${NC}"
    kill $MEDIA_PID 2>/dev/null && echo -e "${GREEN}✅ Media Service stopped${NC}"
    kill $GATEWAY_PID 2>/dev/null && echo -e "${GREEN}✅ API Gateway stopped${NC}"
    
    rm .service-pids
else
    echo -e "${RED}No PID file found. Killing all Spring Boot processes...${NC}"
    pkill -f "spring-boot:run"
    echo -e "${GREEN}✅ All Spring Boot processes stopped${NC}"
fi

echo ""
echo -e "${GREEN} All services stopped!${NC}"