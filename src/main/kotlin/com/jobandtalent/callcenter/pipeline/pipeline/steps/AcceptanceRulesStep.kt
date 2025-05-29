package com.jobandtalent.callcenter.pipeline.pipeline.steps

import com.jobandtalent.callcenter.pipeline.domain.CommunicationRequest
import com.jobandtalent.callcenter.pipeline.domain.PipelineResult
import com.jobandtalent.callcenter.pipeline.domain.RequestStatus
import com.jobandtalent.callcenter.pipeline.pipeline.PipelineStep
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(1)
class AcceptanceRulesStep : PipelineStep {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val stepName = "acceptanceRules"
    override val description = "Checks if the communication request meets acceptance criteria"

    override suspend fun execute(request: CommunicationRequest): PipelineResult {
        val updatedRequest = request.updateStatus(RequestStatus.ACCEPTANCE_RULES_CHECKING)

        // Simulate async rule checking
        delay(100)

        // Business logic - check acceptance rules
        val isAccepted = checkAcceptanceRules(updatedRequest)

        return if (isAccepted) {
            logger.info("Acceptance rules passed for request ${request.id}")
            PipelineResult(
                success = true,
                request = updatedRequest
                    .updateStatus(RequestStatus.ACCEPTANCE_RULES_PASSED)
                    .addMetadata("acceptanceRulesResult", "PASSED")
            )
        } else {
            logger.warn("Acceptance rules failed for request ${request.id}")
            PipelineResult(
                success = false,
                request = updatedRequest.updateStatus(RequestStatus.ACCEPTANCE_RULES_FAILED),
                message = "Request does not meet acceptance criteria",
                shouldContinue = false
            )
        }
    }

    private fun checkAcceptanceRules(request: CommunicationRequest): Boolean {
        // Example acceptance rules
        return when {
            request.workerId.isBlank() -> false
            request.useCase.isBlank() -> false
            request.payload.isEmpty() -> false
            // Add more business rules here
            else -> true
        }
    }
}
