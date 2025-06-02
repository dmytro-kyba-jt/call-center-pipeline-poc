package com.jobandtalent.callcenter.pipeline.pipeline.impl

import com.jobandtalent.callcenter.pipeline.domain.CommunicationRequest
import com.jobandtalent.callcenter.pipeline.domain.PipelineResult
import com.jobandtalent.callcenter.pipeline.domain.RequestStatus
import com.jobandtalent.callcenter.pipeline.pipeline.CommunicationPipelineExecutor
import com.jobandtalent.callcenter.pipeline.pipeline.PipelineStepRegistry
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class CommunicationPipelineExecutorImpl(
    private val stepRegistry: PipelineStepRegistry
) : CommunicationPipelineExecutor {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun execute(request: CommunicationRequest): PipelineResult {
        return executeFromStep(request, "")
    }

    override suspend fun executeFromStep(request: CommunicationRequest, fromStep: String): PipelineResult {
        return withContext(MDCContext()) {
            MDC.put("requestId", request.id)
            MDC.put("workerId", request.workerId)

            try {
                logger.info("Starting pipeline execution for request ${request.id}")

                val steps = stepRegistry.getOrderedSteps()
                val startIndex = if (fromStep.isBlank()) 0 else {
                    steps.indexOfFirst { it.stepName == fromStep }.takeIf { it >= 0 } ?: 0
                }

                var currentRequest = request

                for (i in startIndex until steps.size) {
                    val step = steps[i]

                    if (!step.canExecute(currentRequest)) {
                        logger.debug("Skipping step ${step.stepName} - canExecute returned false")
                        continue
                    }

                    logger.info("Executing step: ${step.stepName}")

                    try {
                        val result = step.execute(currentRequest)

                        if (result.success) {
                            currentRequest = result.request
                            step.onSuccess(currentRequest)
                            logger.info("Step ${step.stepName} completed successfully")

                            if (!result.shouldContinue) {
                                logger.info("Pipeline execution stopped at step ${step.stepName} as requested")
                                return@withContext result
                            }
                        } else {
                            step.onFailure(currentRequest, result.error ?: RuntimeException(result.message))
                            logger.error("Step ${step.stepName} failed: ${result.message}")

                            return@withContext PipelineResult(
                                success = false,
                                request = result.request.updateStatus(RequestStatus.FAILED),
                                message = "Pipeline failed at step ${step.stepName}: ${result.message}",
                                error = result.error
                            )
                        }
                    } catch (e: Exception) {
                        step.onFailure(currentRequest, e)
                        logger.error("Step ${step.stepName} threw exception", e)

                        return@withContext PipelineResult(
                            success = false,
                            request = currentRequest.updateStatus(RequestStatus.FAILED),
                            message = "Pipeline failed at step ${step.stepName}: ${e.message}",
                            error = e
                        )
                    }
                }

                logger.info("Pipeline execution completed successfully for request ${request.id}")

                PipelineResult(
                    success = true,
                    request = currentRequest.updateStatus(RequestStatus.COMPLETED),
                    message = "Pipeline completed successfully"
                )

            } finally {
                MDC.clear()
            }
        }
    }
}
