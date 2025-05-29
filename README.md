# Communication Pipeline

## Overview

The communication pipeline processes `CommunicationRequest` objects through a series of ordered, pluggable steps. It is designed to be flexible, testable, and easily extensible for different use cases.

## Key Components

* **CommunicationRequest**: Core domain object containing communication data (workerId, payload, type, useCase, etc.).
* **PipelineStep**: Interface representing a single unit of work in the pipeline (e.g., validation, storage, scheduling).
* **PipelineStepRegistry**: Registers and stores the execution order of steps.
* **CommunicationPipelineExecutor**: Coordinates the sequential execution of steps and handles success/failure logic.

## Execution Flow

1. **Initialization**: All `PipelineStep` beans are registered via `PipelineConfiguration`.
2. **Processing**:

    * A request is passed to `CommunicationPipelineExecutor.execute()`.
    * Steps execute sequentially in the order they were registered.
    * Each step can:

        * Modify the request
        * Halt the pipeline on failure
        * Skip execution if `canExecute()` returns false
3. **Status Tracking**: The request status is updated throughout the pipeline (e.g., `ACCEPTANCE_RULES_PASSED`, `FAILED`, `COMPLETED`).

## Example Steps

* **AcceptanceRulesStep**: Validates initial request fields.
* **DataStorageStep**: Persists the request.
* **SchedulerStep**: Schedules delivery based on priority.
* **ExclusionRulesStep**: Blocks requests based on business rules.
* **CommunicationProviderStep**: Sends the request to the external provider.

## API Endpoints

* `POST /api/communications`: Submit a new communication request.
* `POST /api/communications/{requestId}/reprocess?stepName=X`: Restart pipeline from a specific step.