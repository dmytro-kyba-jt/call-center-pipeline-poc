package com.jobandtalent.callcenter.pipeline.pipeline

import com.jobandtalent.callcenter.pipeline.domain.CommunicationRequest
import com.jobandtalent.callcenter.pipeline.domain.PipelineResult

interface PipelineStep {
    val stepName: String
    val description: String

    suspend fun execute(request: CommunicationRequest): PipelineResult

    fun canExecute(request: CommunicationRequest): Boolean = true

    fun onSuccess(request: CommunicationRequest) {}

    fun onFailure(request: CommunicationRequest, error: Throwable) {}
}

interface PipelineStepRegistry {
    fun registerStep(step: PipelineStep)
    fun getOrderedSteps(): List<PipelineStep>
    fun getStep(stepName: String): PipelineStep?
}

interface CommunicationPipelineExecutor {
    suspend fun execute(request: CommunicationRequest): PipelineResult
    suspend fun executeFromStep(request: CommunicationRequest, fromStep: String): PipelineResult
}