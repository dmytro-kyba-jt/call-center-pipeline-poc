package com.jobandtalent.callcenter.pipeline.controller

import com.jobandtalent.callcenter.pipeline.domain.CommunicationRequest
import com.jobandtalent.callcenter.pipeline.domain.CommunicationType
import com.jobandtalent.callcenter.pipeline.domain.Priority
import com.jobandtalent.callcenter.pipeline.service.CommunicationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/communications")
class CommunicationController(
    private val communicationService: CommunicationService
) {

    @PostMapping
    suspend fun createCommunicationRequest(
        @RequestBody request: CreateCommunicationRequest
    ): ResponseEntity<*> {
        val communicationRequest = CommunicationRequest(
            workerId = request.workerId,
            communicationType = request.communicationType,
            payload = request.payload,
            useCase = request.useCase,
            priority = request.priority
        )

        val result = communicationService.processCommunicationRequest(communicationRequest)

        return if (result.success) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/{requestId}/reprocess")
    suspend fun reprocessFromStep(
        @PathVariable requestId: String,
        @RequestParam stepName: String,
        @RequestBody request: CommunicationRequest
    ): ResponseEntity<*> {
        val result = communicationService.reprocessFromStep(request, stepName)

        return if (result.success) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }
}

data class CreateCommunicationRequest(
    val workerId: String,
    val communicationType: CommunicationType,
    val payload: Map<String, Any>,
    val useCase: String,
    val priority: Priority = Priority.NORMAL
)
