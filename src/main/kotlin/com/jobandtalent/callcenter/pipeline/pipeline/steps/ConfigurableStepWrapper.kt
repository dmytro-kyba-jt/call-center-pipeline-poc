package com.jobandtalent.callcenter.pipeline.pipeline.steps

import com.jobandtalent.callcenter.pipeline.domain.CommunicationRequest
import com.jobandtalent.callcenter.pipeline.domain.PipelineResult
import com.jobandtalent.callcenter.pipeline.pipeline.PipelineStep

// Wrapper for steps with dynamic configuration
class ConfigurableStepWrapper(
    private val delegate: PipelineStep,
    private val configuration: Map<String, Any>,
    private val condition: String?
) : PipelineStep {

    override val stepName: String get() = delegate.stepName
    override val description: String get() = delegate.description

    override suspend fun execute(request: CommunicationRequest): PipelineResult {
        // Add configuration to request metadata for step to use
        val enhancedRequest = request.copy().apply {
            metadata["stepConfiguration"] = configuration
        }
        return delegate.execute(enhancedRequest)
    }

    override fun canExecute(request: CommunicationRequest): Boolean {
        // Evaluate condition if present
        if (condition != null) {
            // Simple expression evaluation - you could use a more sophisticated engine
            val canExecuteFromCondition = evaluateCondition(condition, request)
            return canExecuteFromCondition && delegate.canExecute(request)
        }
        return delegate.canExecute(request)
    }

    private fun evaluateCondition(condition: String, request: CommunicationRequest): Boolean {
        // Simple condition evaluation - extend as needed
        return when {
            condition.startsWith("priority=") -> {
                val expectedPriority = condition.substringAfter("priority=")
                request.priority.name.equals(expectedPriority, ignoreCase = true)
            }
            condition.startsWith("communicationType=") -> {
                val expectedType = condition.substringAfter("communicationType=")
                request.communicationType.name.equals(expectedType, ignoreCase = true)
            }
            else -> true
        }
    }

    override fun onSuccess(request: CommunicationRequest) = delegate.onSuccess(request)
    override fun onFailure(request: CommunicationRequest, error: Throwable) = delegate.onFailure(request, error)
}
