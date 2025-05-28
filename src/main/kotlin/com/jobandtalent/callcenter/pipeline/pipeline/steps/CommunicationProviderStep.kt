package com.jobandtalent.callcenter.pipeline.pipeline.steps

import com.jobandtalent.callcenter.pipeline.domain.CommunicationRequest
import com.jobandtalent.callcenter.pipeline.domain.PipelineResult
import com.jobandtalent.callcenter.pipeline.domain.RequestStatus
import com.jobandtalent.callcenter.pipeline.pipeline.PipelineStep
import com.jobandtalent.callcenter.pipeline.service.CommunicationProviderService
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(5)
class CommunicationProviderStep(
    private val communicationProviderService: CommunicationProviderService
) : PipelineStep {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val stepName = "communicationProvider"
    override val description = "Sends the communication request to external provider"

    override suspend fun execute(request: CommunicationRequest): PipelineResult {
        return try {
            val result = communicationProviderService.sendCommunication(request)

            logger.info("Communication sent to provider for request ${request.id}")

            PipelineResult(
                success = true,
                request = request
                    .updateStatus(RequestStatus.SENT_TO_PROVIDER)
                    .addMetadata("providerResponse", result)
                    .addMetadata("sentAt", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            logger.error("Failed to send communication for request ${request.id}", e)
            PipelineResult(
                success = false,
                request = request,
                message = "Failed to send communication: ${e.message}",
                error = e
            )
        }
    }
}
