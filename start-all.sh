#!/bin/bash

# Start All Argus Services
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Starting All Argus Services${NC}"

# Create log directory
mkdir -p logs

# Function to check if port is in use
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null ; then
        echo -e "${YELLOW}⚠️  Port $port is already in use${NC}"
        return 1
    fi
    return 0
}

# Function to wait for service to be ready
wait_for_service() {
    local url=$1
    local service_name=$2
    local max_attempts=30
    
    echo -e "${BLUE}⏳ Waiting for $service_name to be ready...${NC}"
    
    for i in $(seq 1 $max_attempts); do
        if curl -s "$url" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ $service_name is ready!${NC}"
            return 0
        fi
        sleep 2
        printf "."
    done
    
    echo ""
    echo -e "${RED}❌ $service_name failed to start within ${max_attempts} attempts${NC}"
    return 1
}

# Check if JARs exist
CORE_JAR="argus-core/target/argus-core-1.0.0.jar"
SCHEDULER_JAR="argus-ui-scheduler/target/argus-ui-scheduler-1.0.0.jar"

if [ ! -f "$CORE_JAR" ] || [ ! -f "$SCHEDULER_JAR" ]; then
    echo -e "${RED}❌ JAR files not found. Building project first...${NC}"
    ./build.sh
fi

# Check ports
if ! check_port 8080; then
    echo -e "${RED}❌ Port 8080 is in use. Please stop other services first.${NC}"
    exit 1
fi

if ! check_port 8081; then
    echo -e "${RED}❌ Port 8081 is in use. Please stop other services first.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Ports 8080 and 8081 are available${NC}"

# Start Argus Core in background
echo -e "${BLUE}🔧 Starting Argus Core API...${NC}"
SPRING_PROFILES_ACTIVE=local \
ANTHROPIC_API_KEY="YOUR_ANTHROPIC_API_KEY" \
nohup java -jar \
    -Dserver.port=8080 \
    -Dspring.profiles.active=local \
    -Dlogging.file.name=logs/argus-core.log \
    "$CORE_JAR" > logs/argus-core-console.log 2>&1 &

CORE_PID=$!
echo -e "${GREEN}✅ Argus Core started with PID: $CORE_PID${NC}"

# Wait for Core API to be ready
if ! wait_for_service "http://localhost:8080/api/v1/anomaly/health" "Argus Core API"; then
    echo -e "${RED}❌ Failed to start Argus Core API${NC}"
    kill $CORE_PID 2>/dev/null || true
    exit 1
fi

# Start Argus UI Scheduler in background
echo -e "${BLUE}🎨 Starting Argus UI Scheduler...${NC}"
SPRING_PROFILES_ACTIVE=local \
ARGUS_CORE_URL="http://localhost:8080" \
nohup java -jar \
    -Dserver.port=8081 \
    -Dspring.profiles.active=local \
    -Dargus.core.url=http://localhost:8080 \
    -Dlogging.file.name=logs/argus-scheduler.log \
    "$SCHEDULER_JAR" > logs/argus-scheduler-console.log 2>&1 &

SCHEDULER_PID=$!
echo -e "${GREEN}✅ Argus Scheduler started with PID: $SCHEDULER_PID${NC}"

# Wait for Scheduler to be ready
if ! wait_for_service "http://localhost:8081/actuator/health" "Argus UI Scheduler"; then
    echo -e "${RED}❌ Failed to start Argus UI Scheduler${NC}"
    kill $CORE_PID 2>/dev/null || true
    kill $SCHEDULER_PID 2>/dev/null || true
    exit 1
fi

# Save PIDs to file for easy cleanup
echo "$CORE_PID" > logs/argus-core.pid
echo "$SCHEDULER_PID" > logs/argus-scheduler.pid

echo ""
echo -e "${GREEN}🎉 All Argus services started successfully!${NC}"
echo ""
echo -e "${BLUE}📋 Service Information:${NC}"
echo -e "   • Argus Core API:       http://localhost:8080"
echo -e "   • Argus Dashboard:      http://localhost:8081"
echo -e "   • API Health:           http://localhost:8080/api/v1/anomaly/health"
echo -e "   • Dashboard Health:     http://localhost:8081/actuator/health"
echo ""
echo -e "${BLUE}📊 Process Information:${NC}"
echo -e "   • Core PID:             $CORE_PID"
echo -e "   • Scheduler PID:        $SCHEDULER_PID"
echo ""
echo -e "${BLUE}📝 Log Files:${NC}"
echo -e "   • Core Logs:            logs/argus-core-console.log"
echo -e "   • Scheduler Logs:       logs/argus-scheduler-console.log"
echo ""
echo -e "${BLUE}🛑 To Stop Services:${NC}"
echo -e "   • Stop all:             ./stop-all.sh"
echo -e "   • View logs:            tail -f logs/argus-*.log"
echo ""
echo -e "${GREEN}🌐 Open Dashboard: http://localhost:8081${NC}"

# Optional: Open dashboard in default browser (macOS/Linux)
if command -v open > /dev/null 2>&1; then
    echo -e "${BLUE}🌐 Opening dashboard in browser...${NC}"
    sleep 3
    open http://localhost:8081
elif command -v xdg-open > /dev/null 2>&1; then
    echo -e "${BLUE}🌐 Opening dashboard in browser...${NC}"
    sleep 3
    xdg-open http://localhost:8081
fi