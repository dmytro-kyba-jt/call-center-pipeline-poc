package com.jobandtalent.callcenter.pipeline.controller

import com.jobandtalent.callcenter.pipeline.domain.CommunicationRequest
import com.jobandtalent.callcenter.pipeline.domain.CommunicationType
import com.jobandtalent.callcenter.pipeline.domain.Priority
import com.jobandtalent.callcenter.pipeline.service.CommunicationService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Validated
@RestController
@RequestMapping("/api/communications")
class CommunicationController(
    private val communicationService: CommunicationService
) {

    @PostMapping
    suspend fun createCommunicationRequest(
        @Valid @RequestBody request: CreateCommunicationRequest
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
//    @field:NotBlank(message = "Worker ID must not be blank")
    val workerId: String,

    val communicationType: CommunicationType,

    val payload: Map<String, Any>,

//    @field:NotBlank(message = "Use case must not be blank")
    val useCase: String,

    val priority: Priority = Priority.NORMAL
)

