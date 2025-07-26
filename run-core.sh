#!/bin/bash

# Argus Core Service Runner
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Starting Argus Core API Service${NC}"

# Check if JAR exists
CORE_JAR="argus-core/target/argus-core-1.0.0.jar"

if [ ! -f "$CORE_JAR" ]; then
    echo -e "${RED}❌ JAR file not found: $CORE_JAR${NC}"
    echo -e "${YELLOW}💡 Run ./build.sh first to build the application${NC}"
    exit 1
fi

# Check if port 8080 is already in use
if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null ; then
    echo -e "${YELLOW}⚠️  Port 8080 is already in use${NC}"
    echo -e "Process using port 8080:"
    lsof -Pi :8080 -sTCP:LISTEN
    echo ""
    read -p "Kill the process and continue? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${BLUE}🔪 Killing process on port 8080...${NC}"
        lsof -ti:8080 | xargs kill -9
        sleep 2
    else
        echo -e "${RED}❌ Cannot start service on port 8080${NC}"
        exit 1
    fi
fi

# Set environment variables
export SPRING_PROFILES_ACTIVE=local
export ANTHROPIC_API_KEY="YOUR_ANTHROPIC_API_KEY"

echo -e "${GREEN}✅ Environment configured${NC}"
echo -e "${BLUE}📊 Service Configuration:${NC}"
echo -e "   • Port: 8080"
echo -e "   • Profile: local"
echo -e "   • JAR: $CORE_JAR"
echo -e "   • Anthropic API: Configured"

echo ""
echo -e "${GREEN}🎯 Starting Argus Core API...${NC}"
echo -e "${YELLOW}📝 Logs will appear below. Press Ctrl+C to stop the service.${NC}"
echo ""

# Create log directory
mkdir -p logs

# Start the service
java -jar \
    -Dserver.port=8080 \
    -Dspring.profiles.active=local \
    -Dlogging.file.name=logs/argus-core.log \
    "$CORE_JAR" | tee logs/argus-core-console.log

echo ""
echo -e "${BLUE}ℹ️  Argus Core API stopped${NC}"