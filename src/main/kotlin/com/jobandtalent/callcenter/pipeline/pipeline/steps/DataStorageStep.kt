package com.jobandtalent.callcenter.pipeline.pipeline.steps

import com.jobandtalent.callcenter.pipeline.domain.CommunicationRequest
import com.jobandtalent.callcenter.pipeline.domain.PipelineResult
import com.jobandtalent.callcenter.pipeline.domain.RequestStatus
import com.jobandtalent.callcenter.pipeline.pipeline.PipelineStep
import com.jobandtalent.callcenter.pipeline.repository.CommunicationRequestRepository
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(2)
class DataStorageStep(
    private val repository: CommunicationRequestRepository
) : PipelineStep {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val stepName = "dataStorage"
    override val description = "Stores the communication request in the database"

    override suspend fun execute(request: CommunicationRequest): PipelineResult {
        return try {
            // Simulate async database operation
            delay(50)

            repository.save(request)

            logger.info("Request ${request.id} stored successfully")

            PipelineResult(
                success = true,
                request = request
                    .updateStatus(RequestStatus.DATA_STORED)
                    .addMetadata("storedAt", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            logger.error("Failed to store request ${request.id}", e)
            PipelineResult(
                success = false,
                request = request,
                message = "Failed to store request: ${e.message}",
                error = e
            )
        }
    }
}

