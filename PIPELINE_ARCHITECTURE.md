# Pipeline Architecture: Static vs Configurable Approach

## Overview

This document explains the key differences between the **original static pipeline approach** and the new **configurable dynamic pipeline approach** in the Communication Pipeline system.

## Original Approach (Static Pipeline)

The original classes used a **static, annotation-based** pipeline configuration.

### Architecture Components

- **PipelineStepRegistryImpl**: Steps registered and ordered using Spring's `@Order` annotation
- **CommunicationPipelineExecutorImpl**: Executes ALL registered steps in the same order for EVERY request
- **Pipeline Steps**: Each step has a fixed execution order defined at compile time

### Implementation Example

```kotlin
@Component
@Order(1)  // Always executes first
class AcceptanceRulesStep : PipelineStep { ... }

@Component  
@Order(2)  // Always executes second
class DataStorageStep : PipelineStep { ... }

@Component
@Order(3)  // Always executes third
class SchedulerStep : PipelineStep { ... }
```

### Execution Flow

```
Every Request → AcceptanceRules → DataStorage → Scheduler → ExclusionRules → Provider
```

### Limitations

- **One-size-fits-all**: Same pipeline for every use case
- **Hard-coded ordering**: Can't change step order without recompiling
- **No conditional logic**: Can't skip steps based on business rules
- **No dynamic configuration**: Can't pass different parameters to steps
- **No runtime management**: Pipeline changes require code deployment

---

## Configurable Approach (Dynamic Pipeline)

The new **Configurable** classes introduce **dynamic, database-driven** pipeline configuration.

### Architecture Components

#### 1. ConfigurablePipelineStepRegistry
```kotlin
// Can return different step configurations per use case
fun getOrderedStepsForUseCase(useCase: String): List<PipelineStep> {
    val config = configRepository.findByUseCase(useCase)
    // Returns steps based on database configuration, not annotations
}
```

#### 2. ConfigurableCommunicationPipelineExecutor
```kotlin
// Uses use-case specific step ordering
val steps = stepRegistry.getOrderedStepsForUseCase(request.useCase)
```

#### 3. ConfigurableStepWrapper
```kotlin
// Adds dynamic behavior to existing steps
class ConfigurableStepWrapper(
    private val delegate: PipelineStep,
    private val configuration: Map<String, Any>,  // Dynamic config!
    private val condition: String?                // Conditional execution!
)
```

### Key Differences

## 1. Database-Driven Configuration

### Old Way: Fixed in Code
```kotlin
@Order(1) AcceptanceRules
@Order(2) DataStorage  
@Order(3) Scheduler
```

### New Way: Stored in Database
```kotlin
// Database record for "urgent-notification" use case:
UseCasePipelineConfig(
    useCase = "urgent-notification",
    steps = [
        PipelineStepConfig("acceptanceRules", order=1, enabled=true),
        PipelineStepConfig("scheduler", order=2, enabled=true),        // Skip DataStorage!
        PipelineStepConfig("communicationProvider", order=3, enabled=true)
    ]
)

// Database record for "regular-notification" use case:  
UseCasePipelineConfig(
    useCase = "regular-notification", 
    steps = [
        PipelineStepConfig("acceptanceRules", order=1, enabled=true),
        PipelineStepConfig("dataStorage", order=2, enabled=true),
        PipelineStepConfig("exclusionRules", order=3, enabled=true),   // Extra step!
        PipelineStepConfig("scheduler", order=4, enabled=true),
        PipelineStepConfig("communicationProvider", order=5, enabled=true)
    ]
)
```

## 2. Conditional Step Execution

### Old Way
All steps always run (if `canExecute()` returns true)

### New Way
Steps can have conditions:
```kotlin
PipelineStepConfig(
    stepName = "exclusionRules",
    condition = "priority=HIGH",           // Only run for HIGH priority
    configuration = mapOf("strictMode" to true)
)
```

**Supported Conditions:**
- `priority=HIGH|NORMAL|LOW|URGENT`
- `communicationType=CALL|SMS|EMAIL|PUSH_NOTIFICATION`
- Custom condition expressions

## 3. Dynamic Step Configuration

### Old Way
Steps have fixed behavior

### New Way
Steps receive runtime configuration:
```kotlin
// Step receives different config based on use case
PipelineStepConfig(
    stepName = "scheduler", 
    configuration = mapOf(
        "maxRetries" to 3,
        "timeoutSeconds" to 30,
        "strategy" to "exponential-backoff"
    )
)
```

The configuration is passed to steps via request metadata:
```kotlin
override suspend fun execute(request: CommunicationRequest): PipelineResult {
    val config = request.metadata["stepConfiguration"] as? Map<String, Any>
    val maxRetries = config?.get("maxRetries") as? Int ?: 1
    // Use dynamic configuration...
}
```

## 4. Runtime Management

The configurable approach adds **REST APIs** to manage pipeline configurations:

### Create New Pipeline Configuration
```http
POST /api/pipeline-config
Content-Type: application/json

{
  "useCase": "emergency-alert",
  "description": "Fast pipeline for emergency notifications",
  "steps": [
    {"stepName": "acceptanceRules", "order": 1, "isEnabled": true},
    {"stepName": "communicationProvider", "order": 2, "isEnabled": true}
  ]
}
```

### Update Existing Configuration
```http
PUT /api/pipeline-config/emergency-alert
Content-Type: application/json

{
  "description": "Updated emergency pipeline",
  "steps": [
    {"stepName": "acceptanceRules", "order": 1, "isEnabled": true},
    {"stepName": "scheduler", "order": 2, "isEnabled": true},
    {"stepName": "communicationProvider", "order": 3, "isEnabled": true}
  ]
}
```

### Get Available Steps
```http
GET /api/pipeline-config/available-steps
```

## Execution Flow Comparison

### Scenario: Processing urgent vs regular notification

#### Old Approach (Same pipeline for both)
```
Request A (urgent)  → AcceptanceRules → DataStorage → Scheduler → ExclusionRules → Provider
Request B (regular) → AcceptanceRules → DataStorage → Scheduler → ExclusionRules → Provider
```

#### New Approach (Different pipelines)
```
Request A (urgent)  → AcceptanceRules → Provider  
                     (Skip storage, scheduling, exclusions for speed!)

Request B (regular) → AcceptanceRules → DataStorage → ExclusionRules → Scheduler → Provider
                     (Full pipeline with all safeguards)
```

## Benefits of Configurable Approach

| Benefit | Description |
|---------|-------------|
| **Flexibility** | Different pipelines for different business scenarios |
| **Runtime Changes** | Modify pipelines without redeployment |
| **A/B Testing** | Test different pipeline configurations |
| **Business Rules** | Add/remove steps based on conditions |
| **Performance** | Skip unnecessary steps for certain use cases |
| **Compliance** | Audit pipeline changes through database |
| **Rollback** | Easy to revert to previous configurations |
| **Scalability** | Add new use cases without code changes |

## Backward Compatibility

The configurable approach **falls back** to the old annotation-based approach when no database configuration exists:

```kotlin
fun getOrderedStepsForUseCase(useCase: String): List<PipelineStep> {
    val config = configRepository.findByUseCase(useCase)
    
    return if (config != null) {
        // Use database configuration (new way)
        buildConfigurableSteps(config)
    } else {
        // Fallback to annotation ordering (old way)
        getOrderedSteps()  
    }
}
```

This allows for **gradual migration** - you can start using configurable pipelines for new use cases while keeping existing ones unchanged.

## Use Case Examples

### Emergency Notifications
```json
{
  "useCase": "emergency-alert",
  "steps": [
    {"stepName": "acceptanceRules", "order": 1},
    {"stepName": "communicationProvider", "order": 2}
  ]
}
```
**Result**: Fastest possible delivery, skipping storage and complex scheduling.

### Marketing Campaigns
```json
{
  "useCase": "marketing-campaign",
  "steps": [
    {"stepName": "acceptanceRules", "order": 1},
    {"stepName": "dataStorage", "order": 2},
    {"stepName": "exclusionRules", "order": 3, "condition": "priority=NORMAL"},
    {"stepName": "scheduler", "order": 4, "configuration": {"batchSize": 1000}},
    {"stepName": "communicationProvider", "order": 5}
  ]
}
```
**Result**: Full pipeline with batching and exclusion rules for marketing compliance.

### High-Priority Alerts
```json
{
  "useCase": "high-priority-alert",
  "steps": [
    {"stepName": "acceptanceRules", "order": 1},
    {"stepName": "scheduler", "order": 2, "condition": "priority=HIGH"},
    {"stepName": "communicationProvider", "order": 3}
  ]
}
```
**Result**: Skip data storage and exclusion rules, but still schedule for proper timing.

## Migration Strategy

1. **Phase 1**: Deploy configurable system alongside existing system
2. **Phase 2**: Create database configurations for new use cases
3. **Phase 3**: Gradually migrate existing use cases to database configurations
4. **Phase 4**: Eventually deprecate annotation-based approach (optional)

The dual approach ensures zero downtime and risk-free migration.