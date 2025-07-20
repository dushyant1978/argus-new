# Argus - Banner Anomaly Detection System

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-green.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Ready-purple.svg)](https://kubernetes.io/)

**Argus** is a comprehensive microservices system designed to detect anomalies in banner advertisements by analyzing banner images and comparing them with product data from e-commerce APIs. The system leverages AI-powered image analysis and automated scheduling to provide real-time anomaly detection and reporting.

## 🏗️ Architecture

Argus consists of two main microservices:

### 1. **argus-core** (Backend API) - Port 8080
- **Purpose**: Core anomaly detection engine
- **Key Features**:
  - Banner image analysis using Anthropic Claude API
  - Product data fetching from Ajio API
  - Intelligent anomaly detection algorithms
  - RESTful API endpoints
  - Caching for performance optimization

### 2. **argus-ui-scheduler** (UI + Scheduler) - Port 8081
- **Purpose**: Web dashboard and automated scheduling
- **Key Features**:
  - Professional web dashboard with Thymeleaf
  - Configurable scheduled scans
  - Page configuration management
  - Real-time reporting and analytics
  - Service health monitoring

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- Docker (optional)
- Kubernetes (optional)

### 1. Build the Project
```bash
git clone <repository-url>
cd argus
./build.sh
```

### 2. Start All Services
```bash
# Start both services automatically
./start-all.sh

# Or start individually
./run-core.sh        # Terminal 1
./run-scheduler.sh   # Terminal 2
```

### 3. Access the Dashboard
- **Web Dashboard**: http://localhost:8081
- **Core API Health**: http://localhost:8080/api/v1/anomaly/health
- **H2 Database Console**: http://localhost:8081/h2-console

### 4. Stop Services
```bash
./stop-all.sh
```

## 📊 API Documentation

### Core API Endpoints

#### POST `/api/v1/anomaly/detect`
Detect anomalies in banner advertisements.

**Request:**
```json
{
  "bannerURL": "https://example.com/banner.jpg",
  "curatedId": 83
}
```

**Response:**
```json
{
  "bannerURL": "https://example.com/banner.jpg",
  "curatedId": 83,
  "bannerInfo": {
    "brands": ["Nike", "Adidas", "Puma"],
    "discountRange": {
      "lower": 20.0,
      "upper": 50.0
    }
  },
  "anomalies": [
    {
      "code": "PROD001",
      "brandName": "Zara",
      "discountPercent": 60.0,
      "anomalyReasons": [
        "Discount 60.0% is above banner maximum 50.0%"
      ]
    }
  ],
  "totalAnomalies": 1,
  "status": "success"
}
```

#### GET `/api/v1/anomaly/health`
Check service health status.

### Scheduler API Endpoints

#### POST `/api/v1/scheduler/run`
Trigger manual full scan across all configured pages.

#### POST `/api/v1/scheduler/run/{pageName}`
Run scan for a specific page.

#### GET `/api/v1/scheduler/reports`
Retrieve all anomaly reports.

#### POST `/api/v1/scheduler/pages`
Create a new page configuration.

**Request:**
```json
{
  "pageName": "Homepage",
  "cmsUrl": "https://cms.example.com/api/homepage"
}
```

## 🔍 How It Works

### Anomaly Detection Process

1. **Banner Analysis**: 
   - Uses Anthropic Claude API with vision capabilities
   - Extracts brand names and discount ranges from banner images
   - Handles various discount formats ("Up to 50%", "20-40%", "30%")

2. **Product Data Fetching**:
   - Retrieves products from Ajio API using curatedId
   - Parses product brands and discount percentages

3. **Anomaly Detection**:
   - **Discount Anomalies**: Products with discounts outside banner range
   - **Brand Anomalies**: Products with brands not mentioned in banner
   - Case-insensitive partial brand matching

4. **Reporting**:
   - Returns first 50 anomalous products
   - Detailed anomaly reasons for each product
   - Comprehensive reporting dashboard

### Scheduled Scanning

- **Configurable Scheduling**: Default every 4 hours (customizable via cron expression)
- **Page Management**: Add/remove/configure multiple pages for scanning
- **CMS Integration**: Fetches banner-product pairs from CMS APIs
- **Automated Reporting**: Generates and stores detailed anomaly reports

## 🛠️ Configuration

### Environment Variables

#### argus-core
- `ANTHROPIC_API_KEY`: Anthropic Claude API key (required)
- `SPRING_PROFILES_ACTIVE`: Active profile (local/kubernetes)

#### argus-ui-scheduler
- `ARGUS_CORE_URL`: URL of the argus-core service
- `ARGUS_SCHEDULER_CRON`: Cron expression for scheduling (default: "0 0 */4 * * *")

### Application Configuration

#### argus-core/src/main/resources/application.yml
```yaml
anthropic:
  api:
    key: ${ANTHROPIC_API_KEY}
    url: https://api.anthropic.com

spring:
  cache:
    type: simple
    cache-names:
      - bannerAnalysis
      - products
```

#### argus-ui-scheduler/src/main/resources/application.yml
```yaml
argus:
  core:
    url: ${ARGUS_CORE_URL:http://localhost:8080}
  scheduler:
    cron: "0 0 */4 * * *"
    enabled: true

spring:
  datasource:
    url: jdbc:h2:mem:argusdb
    driver-class-name: org.h2.Driver
```

## 🐳 Docker Deployment

### Build Docker Images
```bash
./build.sh  # Automatically creates Docker images if Docker is available
```

### Run with Docker Compose
```yaml
# docker-compose.yml
version: '3.8'
services:
  argus-core:
    image: argus/argus-core:1.0.0
    ports:
      - "8080:8080"
    environment:
      - ANTHROPIC_API_KEY=your_api_key_here
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/v1/anomaly/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  argus-scheduler:
    image: argus/argus-scheduler:1.0.0
    ports:
      - "8081:8081"
    environment:
      - ARGUS_CORE_URL=http://argus-core:8080
    depends_on:
      - argus-core
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

```bash
docker-compose up -d
```

## ☸️ Kubernetes Deployment

### Deploy to Kubernetes
```bash
cd k8s
./deploy.sh
```

### Access Services
```bash
# Via NodePort
curl http://localhost:30080/api/v1/anomaly/health
curl http://localhost:30081

# Via Port Forward
kubectl port-forward svc/argus-core-service 8080:8080 -n argus
kubectl port-forward svc/argus-scheduler-service 8081:8081 -n argus
```

### Useful Kubernetes Commands
```bash
# View pods
kubectl get pods -n argus

# View logs
kubectl logs -f deployment/argus-core -n argus
kubectl logs -f deployment/argus-scheduler -n argus

# Scale services
kubectl scale deployment argus-core --replicas=3 -n argus

# Delete deployment
kubectl delete namespace argus
```

## 📈 Monitoring and Health Checks

### Health Endpoints
- **Core Service**: `GET /api/v1/anomaly/health`
- **Scheduler Service**: `GET /actuator/health`

### Metrics
Both services expose Prometheus metrics at `/actuator/prometheus`

### Logging
- Console logs with structured format
- File logging in `logs/` directory
- Different log levels for development and production

## 🧪 Testing

### Manual Testing
```bash
# Test Core API
curl -X POST http://localhost:8080/api/v1/anomaly/detect \
  -H "Content-Type: application/json" \
  -d '{
    "bannerURL": "https://example.com/banner.jpg",
    "curatedId": 83
  }'

# Trigger manual scan
curl -X POST http://localhost:8081/api/v1/scheduler/run
```

### Load Testing
```bash
# Using Apache Bench
ab -n 100 -c 10 -T application/json -p request.json http://localhost:8080/api/v1/anomaly/detect
```

## 🔧 Development

### Project Structure
```
argus/
├── pom.xml                     # Parent Maven configuration
├── argus-core/                 # Core API microservice
│   ├── src/main/java/com/argus/core/
│   │   ├── ArgusCoreApplication.java
│   │   ├── controller/         # REST controllers
│   │   ├── service/           # Business logic
│   │   ├── model/             # Data models
│   │   ├── client/            # External API clients
│   │   └── config/            # Configuration classes
│   └── src/main/resources/
│       └── application.yml    # Core configuration
├── argus-ui-scheduler/        # UI + Scheduler microservice
│   ├── src/main/java/com/argus/scheduler/
│   │   ├── ArgusSchedulerApplication.java
│   │   ├── controller/        # Web and API controllers
│   │   ├── service/          # Business logic
│   │   ├── model/            # JPA entities
│   │   ├── repository/       # Data repositories
│   │   └── client/           # External API clients
│   └── src/main/resources/
│       ├── application.yml   # Scheduler configuration
│       ├── templates/        # Thymeleaf templates
│       └── static/          # CSS, JS, images
├── k8s/                      # Kubernetes manifests
├── logs/                     # Application logs
└── *.sh                     # Build and run scripts
```

### Adding New Features

1. **New Anomaly Detection Rule**:
   - Modify `AnomalyDetectionService.java`
   - Add new anomaly reason logic
   - Update response model if needed

2. **New Dashboard Page**:
   - Add controller method in `DashboardController.java`
   - Create Thymeleaf template in `templates/`
   - Update navigation in existing templates

3. **New API Endpoint**:
   - Add method to appropriate controller
   - Update API documentation
   - Add integration tests

### Code Quality

- **Checkstyle**: Enforce coding standards
- **SpotBugs**: Static analysis for bug detection
- **JaCoCo**: Code coverage reporting
- **SonarQube**: Code quality analysis

## 🚨 Troubleshooting

### Common Issues

#### 1. Port Already in Use
```bash
# Kill processes on ports 8080/8081
./stop-all.sh
# Or manually
lsof -ti:8080 | xargs kill -9
lsof -ti:8081 | xargs kill -9
```

#### 2. Anthropic API Issues
- Verify API key is correct and active
- Check API rate limits and quotas
- Review API key permissions

#### 3. Memory Issues
```bash
# Increase JVM heap size
java -Xmx2g -jar argus-core-1.0.0.jar
```

#### 4. Database Issues (H2)
- Access H2 console: http://localhost:8081/h2-console
- JDBC URL: `jdbc:h2:mem:argusdb`
- Username: `sa`, Password: `password`

### Debug Mode
```bash
# Enable debug logging
export LOGGING_LEVEL_COM_ARGUS=DEBUG
java -jar argus-core-1.0.0.jar
```

### Performance Tuning

#### JVM Options
```bash
java -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -Xms512m -Xmx2g \
     -jar argus-core-1.0.0.jar
```

#### Cache Configuration
- Banner analysis results are cached by URL
- Product data is cached by curatedId
- Cache TTL is configurable in application.yml

## 📝 API Rate Limits

### Anthropic Claude API
- Default rate limits apply
- Implement exponential backoff for failures
- Consider upgrading API tier for production use

### Ajio Product API
- No authentication required for public endpoints
- Rate limiting handled gracefully with fallback data

## 🔐 Security Considerations

### API Security
- Input validation on all endpoints
- Rate limiting for public APIs
- Secure handling of API keys

### Data Privacy
- No sensitive user data stored
- Temporary caching of public product information
- Secure transmission of all data

### Production Deployment
- Use secrets management (K8s secrets, HashiCorp Vault)
- Enable HTTPS/TLS
- Implement proper authentication/authorization
- Regular security updates

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Update documentation
6. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 📞 Support

For support and questions:
- Create an issue on GitHub
- Check the troubleshooting section above
- Review application logs in `logs/` directory

## 🎯 Future Enhancements

- **Machine Learning**: Implement ML-based anomaly detection
- **Real-time Streaming**: Process banner changes in real-time
- **Advanced Analytics**: More detailed reporting and insights
- **Multi-tenant Support**: Support for multiple organizations
- **API Versioning**: Backward-compatible API evolution
- **Advanced Caching**: Redis integration for distributed caching
- **Notifications**: Email/Slack alerts for critical anomalies
- **A/B Testing**: Compare different detection algorithms

---

**Made with ❤️ by the Argus Team**