#!/bin/bash

# Argus Build Script
set -e

echo "🏗️  Building Argus Banner Anomaly Detection System"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven is not installed or not in PATH${NC}"
    echo "Please install Maven 3.6+ to build the project"
    exit 1
fi

# Check Java version
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java is not installed or not in PATH${NC}"
    echo "Please install Java 17+ to build the project"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | grep -oP 'version "?(1\.)?\K\d+' | head -1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}❌ Java 17 or higher is required (found Java $JAVA_VERSION)${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Java $JAVA_VERSION detected${NC}"
echo -e "${GREEN}✅ Maven $(mvn -version | head -1 | cut -d' ' -f3) detected${NC}"

# Clean previous builds
echo -e "${BLUE}🧹 Cleaning previous builds...${NC}"
mvn clean

# Build the parent project and all modules
echo -e "${BLUE}🔨 Building all modules...${NC}"
mvn compile

# Run tests
echo -e "${BLUE}🧪 Running tests...${NC}"
mvn test

# Package applications
echo -e "${BLUE}📦 Packaging applications...${NC}"
mvn package -DskipTests

# Verify builds
echo -e "${BLUE}🔍 Verifying builds...${NC}"

CORE_JAR="argus-core/target/argus-core-1.0.0.jar"
SCHEDULER_JAR="argus-ui-scheduler/target/argus-ui-scheduler-1.0.0.jar"

if [ -f "$CORE_JAR" ]; then
    echo -e "${GREEN}✅ Argus Core JAR built successfully: $CORE_JAR${NC}"
    CORE_SIZE=$(du -h "$CORE_JAR" | cut -f1)
    echo -e "   Size: $CORE_SIZE"
else
    echo -e "${RED}❌ Argus Core JAR not found${NC}"
    exit 1
fi

if [ -f "$SCHEDULER_JAR" ]; then
    echo -e "${GREEN}✅ Argus Scheduler JAR built successfully: $SCHEDULER_JAR${NC}"
    SCHEDULER_SIZE=$(du -h "$SCHEDULER_JAR" | cut -f1)
    echo -e "   Size: $SCHEDULER_SIZE"
else
    echo -e "${RED}❌ Argus Scheduler JAR not found${NC}"
    exit 1
fi

# Create Docker images (if Docker is available)
if command -v docker &> /dev/null; then
    echo -e "${BLUE}🐳 Creating Docker images...${NC}"
    
    # Create Dockerfile for argus-core
    cat > argus-core/Dockerfile << EOF
FROM openjdk:17-jre-slim

WORKDIR /app

COPY target/argus-core-1.0.0.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/api/v1/anomaly/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

    # Create Dockerfile for argus-ui-scheduler
    cat > argus-ui-scheduler/Dockerfile << EOF
FROM openjdk:17-jre-slim

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY target/argus-ui-scheduler-1.0.0.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

    # Build Docker images
    cd argus-core
    docker build -t argus/argus-core:1.0.0 .
    echo -e "${GREEN}✅ Docker image built: argus/argus-core:1.0.0${NC}"
    cd ..
    
    cd argus-ui-scheduler
    docker build -t argus/argus-scheduler:1.0.0 .
    echo -e "${GREEN}✅ Docker image built: argus/argus-scheduler:1.0.0${NC}"
    cd ..
    
    echo -e "${GREEN}✅ Docker images created successfully${NC}"
    
    # Show Docker images
    echo -e "${BLUE}📋 Available Docker images:${NC}"
    docker images | grep argus
else
    echo -e "${YELLOW}⚠️  Docker not found. Skipping Docker image creation.${NC}"
fi

echo ""
echo -e "${GREEN}🎉 Build completed successfully!${NC}"
echo ""
echo -e "${BLUE}📋 Summary:${NC}"
echo -e "   • Argus Core API: $CORE_JAR ($CORE_SIZE)"
echo -e "   • Argus UI Scheduler: $SCHEDULER_JAR ($SCHEDULER_SIZE)"

if command -v docker &> /dev/null; then
    echo -e "   • Docker images: argus/argus-core:1.0.0, argus/argus-scheduler:1.0.0"
fi

echo ""
echo -e "${BLUE}🚀 Next steps:${NC}"
echo -e "   • Start core service:      ./run-core.sh"
echo -e "   • Start scheduler service: ./run-scheduler.sh"
echo -e "   • Deploy to Kubernetes:    cd k8s && ./deploy.sh"
echo -e "   • View dashboard:          http://localhost:8081"