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
@Order(4) // NEW STEP - Easily inserted between existing steps
class ExclusionRulesStep : PipelineStep {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val stepName = "exclusionRules"
    override val description = "Checks if the communication request should be excluded"

    override suspend fun execute(request: CommunicationRequest): PipelineResult {
        val updatedRequest = request.updateStatus(RequestStatus.EXCLUSION_RULES_CHECKING)

        // Simulate async exclusion rule checking
        delay(80)

        val isExcluded = checkExclusionRules(updatedRequest)

        return if (!isExcluded) {
            logger.info("Exclusion rules passed for request ${request.id}")
            PipelineResult(
                success = true,
                request = updatedRequest
                    .updateStatus(RequestStatus.EXCLUSION_RULES_PASSED)
                    .addMetadata("exclusionRulesResult", "PASSED")
            )
        } else {
            logger.warn("Request ${request.id} excluded by exclusion rules")
            PipelineResult(
                success = false,
                request = updatedRequest.updateStatus(RequestStatus.EXCLUSION_RULES_FAILED),
                message = "Request excluded by exclusion rules",
                shouldContinue = false
            )
        }
    }

    private fun checkExclusionRules(request: CommunicationRequest): Boolean {
        // Example exclusion rules
        return when {
            // Check if worker is in do-not-contact list
            request.workerId in listOf("blocked-worker-1", "blocked-worker-2") -> true
            // Check if use case is temporarily disabled
            request.useCase in listOf("disabled-case-1") -> true
            // Check for recent communication attempts
            isRecentCommunicationAttempt(request) -> true
            else -> false
        }
    }

    private fun isRecentCommunicationAttempt(request: CommunicationRequest): Boolean {
        // In real implementation, this would check the database
        // For POC, we'll use a simple rule
        return request.metadata["lastCommunicationMinutesAgo"]?.let {
            ((it as? Int) ?: 0) < 60
        } ?: false
    }
}
