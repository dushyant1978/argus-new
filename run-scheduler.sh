#!/bin/bash

# Argus UI Scheduler Service Runner
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Starting Argus UI Scheduler Service${NC}"

# Check if JAR exists
SCHEDULER_JAR="argus-ui-scheduler/target/argus-ui-scheduler-1.0.0.jar"

if [ ! -f "$SCHEDULER_JAR" ]; then
    echo -e "${RED}❌ JAR file not found: $SCHEDULER_JAR${NC}"
    echo -e "${YELLOW}💡 Run ./build.sh first to build the application${NC}"
    exit 1
fi

# Check if port 8081 is already in use
if lsof -Pi :8081 -sTCP:LISTEN -t >/dev/null ; then
    echo -e "${YELLOW}⚠️  Port 8081 is already in use${NC}"
    echo -e "Process using port 8081:"
    lsof -Pi :8081 -sTCP:LISTEN
    echo ""
    read -p "Kill the process and continue? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${BLUE}🔪 Killing process on port 8081...${NC}"
        lsof -ti:8081 | xargs kill -9
        sleep 2
    else
        echo -e "${RED}❌ Cannot start service on port 8081${NC}"
        exit 1
    fi
fi

# Check if argus-core is running
echo -e "${BLUE}🔍 Checking for Argus Core API...${NC}"
CORE_HEALTH_URL="http://localhost:8080/api/v1/anomaly/health"

if curl -s "$CORE_HEALTH_URL" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Argus Core API is running${NC}"
else
    echo -e "${YELLOW}⚠️  Argus Core API is not running${NC}"
    echo -e "${BLUE}💡 Starting Argus Core in the background...${NC}"
    
    # Start core service in background
    if [ -f "argus-core/target/argus-core-1.0.0.jar" ]; then
        nohup java -jar \
            -Dserver.port=8080 \
            -Dspring.profiles.active=local \
            argus-core/target/argus-core-1.0.0.jar > logs/argus-core-bg.log 2>&1 &
        
        CORE_PID=$!
        echo -e "${GREEN}✅ Argus Core started with PID: $CORE_PID${NC}"
        
        # Wait for core service to be ready
        echo -e "${BLUE}⏳ Waiting for Argus Core to be ready...${NC}"
        for i in {1..30}; do
            if curl -s "$CORE_HEALTH_URL" > /dev/null 2>&1; then
                echo -e "${GREEN}✅ Argus Core is ready!${NC}"
                break
            fi
            sleep 2
            echo -n "."
        done
        echo ""
    else
        echo -e "${RED}❌ Argus Core JAR not found. Please run ./build.sh first${NC}"
        exit 1
    fi
fi

# Set environment variables
export SPRING_PROFILES_ACTIVE=local
export ARGUS_CORE_URL="http://localhost:8080"

echo -e "${GREEN}✅ Environment configured${NC}"
echo -e "${BLUE}📊 Service Configuration:${NC}"
echo -e "   • Port: 8081"
echo -e "   • Profile: local"
echo -e "   • JAR: $SCHEDULER_JAR"
echo -e "   • Core API URL: http://localhost:8080"

echo ""
echo -e "${GREEN}🎯 Starting Argus UI Scheduler...${NC}"
echo -e "${YELLOW}📝 Logs will appear below. Press Ctrl+C to stop the service.${NC}"
echo ""

# Create log directory
mkdir -p logs

# Cleanup function
cleanup() {
    echo ""
    echo -e "${BLUE}🛑 Shutting down services...${NC}"
    
    # Kill background core service if we started it
    if [ ! -z "$CORE_PID" ]; then
        echo -e "${BLUE}🔪 Stopping background Argus Core (PID: $CORE_PID)...${NC}"
        kill $CORE_PID 2>/dev/null || true
    fi
    
    echo -e "${GREEN}✅ Cleanup completed${NC}"
    exit 0
}

# Set up trap for cleanup on script exit
trap cleanup EXIT INT TERM

# Start the service
java -jar \
    -Dserver.port=8081 \
    -Dspring.profiles.active=local \
    -Dargus.core.url=http://localhost:8080 \
    -Dlogging.file.name=logs/argus-scheduler.log \
    "$SCHEDULER_JAR" | tee logs/argus-scheduler-console.log