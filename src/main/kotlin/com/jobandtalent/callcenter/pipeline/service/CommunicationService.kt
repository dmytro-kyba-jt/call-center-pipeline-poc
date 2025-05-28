package com.jobandtalent.callcenter.pipeline.service

import com.jobandtalent.callcenter.pipeline.domain.CommunicationRequest
import com.jobandtalent.callcenter.pipeline.domain.PipelineResult
import com.jobandtalent.callcenter.pipeline.pipeline.CommunicationPipelineExecutor
import org.springframework.stereotype.Service

@Service
class CommunicationService(
    private val pipelineExecutor: CommunicationPipelineExecutor
) {

    suspend fun processCommunicationRequest(request: CommunicationRequest): PipelineResult {
        return pipelineExecutor.execute(request)
    }

    suspend fun reprocessFromStep(request: CommunicationRequest, stepName: String): PipelineResult {
        return pipelineExecutor.executeFromStep(request, stepName)
    }
}
