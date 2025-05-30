package com.jobandtalent.callcenter.pipeline.domain

import java.time.LocalDateTime
import java.util.UUID

data class CommunicationRequest(
    val id: String = UUID.randomUUID().toString(),
    val workerId: String,
    val communicationType: CommunicationType,
    val payload: Map<String, Any>,
    val useCase: String,
    val priority: Priority = Priority.NORMAL,
    var status: RequestStatus = RequestStatus.RECEIVED,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    val metadata: MutableMap<String, Any> = mutableMapOf()
) {
    fun updateStatus(newStatus: RequestStatus): CommunicationRequest {
        return this.copy(
            status = newStatus,
            updatedAt = LocalDateTime.now()
        ).also {
            it.metadata["statusHistory"] = (metadata["statusHistory"] as? MutableList<String> ?: mutableListOf()).apply {
                add("${LocalDateTime.now()}: $newStatus")
            }
        }
    }

    fun addMetadata(key: String, value: Any): CommunicationRequest {
        metadata[key] = value
        return this
    }
}

enum class CommunicationType {
    CALL, SMS, EMAIL, PUSH_NOTIFICATION
}

enum class Priority {
    LOW, NORMAL, HIGH, URGENT
}

enum class RequestStatus {
    RECEIVED,
    ACCEPTANCE_RULES_CHECKING,
    ACCEPTANCE_RULES_PASSED,
    ACCEPTANCE_RULES_FAILED,
    DATA_STORED,
    SCHEDULED,
    EXCLUSION_RULES_CHECKING,
    EXCLUSION_RULES_PASSED,
    EXCLUSION_RULES_FAILED,
    SENT_TO_PROVIDER,
    PROVIDER_RESPONSE_RECEIVED,
    COMPLETED,
    FAILED,
    RETRY_SCHEDULED
}

data class PipelineResult(
    val success: Boolean,
    val request: CommunicationRequest,
    val message: String = "",
    val shouldContinue: Boolean = true,
    val error: Throwable? = null
) {
    companion object {
        fun success(
            request: CommunicationRequest,
            message: String = "",
            shouldContinue: Boolean = true
        ): PipelineResult = PipelineResult(
            success = true,
            request = request,
            message = message,
            shouldContinue = shouldContinue,
            error = null
        )

        fun failure(
            request: CommunicationRequest,
            message: String = "",
            error: Throwable? = null,
            shouldContinue: Boolean = false
        ): PipelineResult = PipelineResult(
            success = false,
            request = request,
            message = message,
            shouldContinue = shouldContinue,
            error = error
        )

        fun stopPipeline(
            request: CommunicationRequest,
            message: String = ""
        ): PipelineResult = PipelineResult(
            success = true,
            request = request,
            message = message,
            shouldContinue = false,
            error = null
        )
    }
}

// Domain models for pipeline configuration
data class UseCasePipelineConfig(
    val id: String = UUID.randomUUID().toString(),
    val useCase: String,
    val description: String,
    val isActive: Boolean = true,
    val steps: List<PipelineStepConfig>,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

data class PipelineStepConfig(
    val stepName: String,
    val order: Int,
    val isEnabled: Boolean = true,
    val condition: String? = null, // Optional condition expression
    val configuration: Map<String, Any> = emptyMap()
)