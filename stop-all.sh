#!/bin/bash

# Stop All Argus Services
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🛑 Stopping All Argus Services${NC}"

# Function to stop process by PID file
stop_by_pid_file() {
    local pid_file=$1
    local service_name=$2
    
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        echo -e "${BLUE}🔪 Stopping $service_name (PID: $pid)...${NC}"
        
        if kill -0 "$pid" 2>/dev/null; then
            kill "$pid"
            
            # Wait for process to stop
            for i in {1..10}; do
                if ! kill -0 "$pid" 2>/dev/null; then
                    echo -e "${GREEN}✅ $service_name stopped${NC}"
                    rm -f "$pid_file"
                    return 0
                fi
                sleep 1
            done
            
            # Force kill if still running
            echo -e "${YELLOW}⚠️  Force killing $service_name...${NC}"
            kill -9 "$pid" 2>/dev/null || true
            rm -f "$pid_file"
            echo -e "${GREEN}✅ $service_name force stopped${NC}"
        else
            echo -e "${YELLOW}⚠️  $service_name process not found${NC}"
            rm -f "$pid_file"
        fi
    else
        echo -e "${YELLOW}⚠️  PID file not found for $service_name${NC}"
    fi
}

# Function to stop process by port
stop_by_port() {
    local port=$1
    local service_name=$2
    
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null ; then
        echo -e "${BLUE}🔪 Stopping process on port $port ($service_name)...${NC}"
        local pid=$(lsof -ti:$port)
        kill $pid 2>/dev/null || true
        sleep 2
        
        if ! lsof -Pi :$port -sTCP:LISTEN -t >/dev/null ; then
            echo -e "${GREEN}✅ Process on port $port stopped${NC}"
        else
            echo -e "${YELLOW}⚠️  Force killing process on port $port...${NC}"
            lsof -ti:$port | xargs kill -9 2>/dev/null || true
            echo -e "${GREEN}✅ Process on port $port force stopped${NC}"
        fi
    else
        echo -e "${GREEN}✅ No process running on port $port${NC}"
    fi
}

# Create logs directory if it doesn't exist
mkdir -p logs

echo -e "${BLUE}🔍 Checking for running Argus services...${NC}"

# Stop by PID files first (cleaner approach)
stop_by_pid_file "logs/argus-core.pid" "Argus Core API"
stop_by_pid_file "logs/argus-scheduler.pid" "Argus UI Scheduler"

# Fallback: Stop by ports (in case PID files don't exist)
stop_by_port 8080 "Argus Core API"
stop_by_port 8081 "Argus UI Scheduler"

# Clean up any remaining processes
echo -e "${BLUE}🧹 Cleaning up remaining processes...${NC}"

# Kill any java processes running argus jars
JAVA_PROCESSES=$(ps aux | grep -i "argus.*\.jar" | grep -v grep | awk '{print $2}' || true)
if [ ! -z "$JAVA_PROCESSES" ]; then
    echo -e "${BLUE}🔪 Stopping remaining Argus Java processes...${NC}"
    echo "$JAVA_PROCESSES" | xargs kill -9 2>/dev/null || true
    echo -e "${GREEN}✅ Remaining processes stopped${NC}"
fi

# Clean up log files older than 7 days
echo -e "${BLUE}🧹 Cleaning up old log files...${NC}"
find logs -name "*.log" -type f -mtime +7 -delete 2>/dev/null || true
find logs -name "*.pid" -type f -delete 2>/dev/null || true

echo ""
echo -e "${GREEN}🎉 All Argus services stopped successfully!${NC}"
echo ""
echo -e "${BLUE}📋 Port Status:${NC}"
if ! lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null ; then
    echo -e "${GREEN}   • Port 8080: Available${NC}"
else
    echo -e "${RED}   • Port 8080: Still in use${NC}"
fi

if ! lsof -Pi :8081 -sTCP:LISTEN -t >/dev/null ; then
    echo -e "${GREEN}   • Port 8081: Available${NC}"
else
    echo -e "${RED}   • Port 8081: Still in use${NC}"
fi

echo ""
echo -e "${BLUE}📝 Log files preserved in logs/ directory${NC}"
echo -e "${BLUE}🚀 To restart services: ./start-all.sh${NC}"