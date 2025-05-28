package com.jobandtalent.callcenter.pipeline.pipeline.steps

import com.jobandtalent.callcenter.pipeline.domain.CommunicationRequest
import com.jobandtalent.callcenter.pipeline.domain.PipelineResult
import com.jobandtalent.callcenter.pipeline.domain.Priority
import com.jobandtalent.callcenter.pipeline.domain.RequestStatus
import com.jobandtalent.callcenter.pipeline.pipeline.PipelineStep
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
@Order(3)
class SchedulerStep : PipelineStep {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val stepName = "scheduler"
    override val description = "Schedules the communication request for processing"

    override suspend fun execute(request: CommunicationRequest): PipelineResult {
        // Simulate async scheduling
        delay(30)

        val scheduledTime = calculateScheduledTime(request)

        logger.info("Request ${request.id} scheduled for $scheduledTime")

        return PipelineResult(
            success = true,
            request = request
                .updateStatus(RequestStatus.SCHEDULED)
                .addMetadata("scheduledTime", scheduledTime.toString())
                .addMetadata("priority", request.priority.name)
        )
    }

    private fun calculateScheduledTime(request: CommunicationRequest): LocalDateTime {
        // Business logic for scheduling
        return when (request.priority) {
            Priority.URGENT -> LocalDateTime.now()
            Priority.HIGH -> LocalDateTime.now().plusMinutes(5)
            Priority.NORMAL -> LocalDateTime.now().plusMinutes(15)
            Priority.LOW -> LocalDateTime.now().plusHours(1)
        }
    }
}
