# Communication Pipeline

A flexible, configurable communication processing system built with Spring Boot and Kotlin that supports multiple pipeline configurations for different business use cases.

## 🚀 Features

- **Dynamic Pipeline Configuration**: Database-driven pipeline steps and ordering
- **Multiple Execution Strategies**: Static (annotation-based) and Configurable (database-driven) approaches
- **Conditional Step Execution**: Steps can execute based on request properties (priority, communication type, etc.)
- **Runtime Configuration**: Modify pipeline behavior without code deployment
- **RESTful Management APIs**: Create, update, and manage pipeline configurations via REST
- **Async Processing**: Coroutine-based async execution with proper error handling
- **Multiple Communication Types**: Support for CALL, SMS, EMAIL, PUSH_NOTIFICATION
- **Priority-based Processing**: URGENT, HIGH, NORMAL, LOW priority levels
- **Comprehensive Logging**: Request tracing with MDC context
- **Graceful Fallback**: Automatic fallback to static configuration when dynamic config is unavailable

## 📋 Table of Contents

- [Architecture Overview](#architecture-overview)
- [Quick Start](#quick-start)
- [API Documentation](#api-documentation)
- [Pipeline Configuration](#pipeline-configuration)
- [Use Case Examples](#use-case-examples)
- [Development](#development)
- [Testing](#testing)

## 🏗️ Architecture Overview

The system supports two pipeline execution approaches:

### Static Pipeline (Original)
- Fixed pipeline order defined by `@Order` annotations
- Same execution flow for all requests
- Compile-time configuration

### Configurable Pipeline (New)
- Database-driven pipeline configuration
- Different pipelines per use case
- Runtime configuration changes
- Conditional step execution

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Gradle 8.14+

### Running the Application

```bash
# Clone the repository
git clone <repository-url>
cd call-center-pipeline-poc

# Run the application
./gradlew bootRun
```

The application will start on `http://localhost:8080`

### Basic Usage

1. **Send a communication request:**
```bash
curl -X POST http://localhost:8080/api/communications \
  -H "Content-Type: application/json" \
  -d '{
    "workerId": "worker-123",
    "communicationType": "EMAIL",
    "payload": {
      "subject": "Hello",
      "body": "Test message"
    },
    "useCase": "notification",
    "priority": "NORMAL"
  }'
```

2. **Create a custom pipeline configuration:**
```bash
curl -X POST http://localhost:8080/api/pipeline-config \
  -H "Content-Type: application/json" \
  -d '{
    "useCase": "urgent-alert",
    "description": "Fast pipeline for urgent notifications",
    "steps": [
      {"stepName": "acceptanceRules", "order": 1, "isEnabled": true},
      {"stepName": "communicationProvider", "order": 2, "isEnabled": true}
    ]
  }'
```

## 📡 API Documentation

### Communication APIs

#### POST /api/communications
Submit a new communication request for processing.

**Request Body:**
```json
{
  "workerId": "string",
  "communicationType": "CALL|SMS|EMAIL|PUSH_NOTIFICATION", 
  "payload": {},
  "useCase": "string",
  "priority": "LOW|NORMAL|HIGH|URGENT"
}
```

**Response:**
```json
{
  "success": true,
  "request": {
    "id": "uuid",
    "status": "COMPLETED",
    "workerId": "worker-123",
    "metadata": {}
  },
  "message": "Pipeline completed successfully"
}
```

#### POST /api/communications/{requestId}/reprocess
Restart pipeline execution from a specific step.

**Parameters:**
- `requestId`: The ID of the request to reprocess
- `stepName`: Query parameter specifying which step to start from

### Pipeline Configuration APIs

#### GET /api/pipeline-config
Get all pipeline configurations.

#### GET /api/pipeline-config/{useCase}
Get pipeline configuration for a specific use case.

#### POST /api/pipeline-config
Create a new pipeline configuration.

**Request Body:**
```json
{
  "useCase": "string",
  "description": "string", 
  "steps": [
    {
      "stepName": "string",
      "order": 1,
      "isEnabled": true,
      "condition": "priority=HIGH",
      "configuration": {}
    }
  ]
}
```

#### PUT /api/pipeline-config/{useCase}
Update an existing pipeline configuration.

#### DELETE /api/pipeline-config/{useCase}
Delete a pipeline configuration.

#### GET /api/pipeline-config/available-steps
Get list of all available pipeline steps.

## ⚙️ Pipeline Configuration

### Available Pipeline Steps

1. **acceptanceRules** - Validates request meets acceptance criteria
2. **dataStorage** - Persists the request to database
3. **scheduler** - Schedules request based on priority
4. **exclusionRules** - Checks exclusion/blocklist rules
5. **communicationProvider** - Sends to external communication provider

### Step Configuration Options

#### Conditional Execution
Steps can be configured to execute only when certain conditions are met:

```json
{
  "stepName": "exclusionRules",
  "condition": "priority=HIGH",
  "order": 3
}
```

**Supported Conditions:**
- `priority=HIGH|NORMAL|LOW|URGENT`
- `communicationType=CALL|SMS|EMAIL|PUSH_NOTIFICATION`

#### Dynamic Configuration
Pass configuration parameters to steps:

```json
{
  "stepName": "scheduler",
  "configuration": {
    "maxRetries": 3,
    "timeoutSeconds": 30,
    "strategy": "exponential-backoff"
  }
}
```

## 📚 Use Case Examples

### Emergency Alerts (Fast Pipeline)
```json
{
  "useCase": "emergency-alert",
  "description": "Fastest delivery for emergency notifications",
  "steps": [
    {"stepName": "acceptanceRules", "order": 1},
    {"stepName": "communicationProvider", "order": 2}
  ]
}
```

### Marketing Campaigns (Full Pipeline)
```json
{
  "useCase": "marketing-campaign", 
  "description": "Full pipeline with all safeguards",
  "steps": [
    {"stepName": "acceptanceRules", "order": 1},
    {"stepName": "dataStorage", "order": 2},
    {"stepName": "exclusionRules", "order": 3},
    {"stepName": "scheduler", "order": 4, "configuration": {"batchSize": 1000}},
    {"stepName": "communicationProvider", "order": 5}
  ]
}
```

### High-Priority Notifications (Conditional)
```json
{
  "useCase": "priority-notification",
  "description": "Different flow based on priority",
  "steps": [
    {"stepName": "acceptanceRules", "order": 1},
    {"stepName": "exclusionRules", "order": 2, "condition": "priority=NORMAL"},
    {"stepName": "scheduler", "order": 3},
    {"stepName": "communicationProvider", "order": 4}
  ]
}
```

## 🔧 Development

### Project Structure

```
src/
├── main/kotlin/com/jobandtalent/callcenter/pipeline/
│   ├── config/                 # Spring configuration
│   ├── controller/             # REST controllers  
│   ├── domain/                 # Domain models
│   ├── pipeline/
│   │   ├── core.kt            # Core interfaces
│   │   ├── impl/              # Pipeline executors and registries
│   │   └── steps/             # Individual pipeline steps
│   ├── repository/            # Data repositories
│   └── service/               # Business services
└── test/                      # Test files
```

### Key Components

- **CommunicationRequest**: Core domain object
- **PipelineStep**: Interface for individual processing steps
- **PipelineStepRegistry**: Manages step registration and ordering
- **CommunicationPipelineExecutor**: Orchestrates pipeline execution
- **ConfigurableStepWrapper**: Adds dynamic behavior to steps

### Adding New Pipeline Steps

1. Implement the `PipelineStep` interface:
```kotlin
@Component
@Order(6)
class MyCustomStep : PipelineStep {
    override val stepName = "myCustomStep"
    override val description = "Description of what this step does"
    
    override suspend fun execute(request: CommunicationRequest): PipelineResult {
        // Implementation
    }
}
```

2. The step will be automatically registered and available in pipeline configurations.

### Configuration

Application configuration is in `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
  jpa:
    hibernate:
      ddl-auto: update
    
logging:
  level:
    com.jobandtalent.callcenter: DEBUG
```

## 🧪 Testing

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "*CommunicationPipelineTest"

# Run integration tests
./gradlew test --tests "*IntegrationTest"
```

### Test Categories

- **Unit Tests**: Individual component testing
- **Integration Tests**: Full pipeline execution testing
- **Controller Tests**: REST API testing with WebTestClient

### Example Test Cases

- Pipeline execution with valid requests
- Error handling for invalid requests
- Blocked worker exclusion
- Step configuration and conditional execution
- Concurrent request processing

## 🔄 Migration from Static to Configurable

The system supports both approaches simultaneously:

1. **Existing use cases** continue using annotation-based ordering
2. **New use cases** can use database configurations
3. **Gradual migration** by creating database configs for existing use cases
4. **Zero downtime** deployment and migration

## 🚀 Future Enhancements

- [ ] Step retry mechanisms with exponential backoff
- [ ] Pipeline execution metrics and monitoring
- [ ] Advanced condition expressions (SpEL)
- [ ] Pipeline versioning and rollback
- [ ] Real-time pipeline execution monitoring
- [ ] Integration with external workflow engines
- [ ] A/B testing framework for pipeline configurations

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.