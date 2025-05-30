package com.jobandtalent.callcenter.pipeline.controller

import com.jobandtalent.callcenter.pipeline.domain.PipelineStepConfig
import com.jobandtalent.callcenter.pipeline.domain.UseCasePipelineConfig
import com.jobandtalent.callcenter.pipeline.pipeline.impl.ConfigurablePipelineStepRegistry
import com.jobandtalent.callcenter.pipeline.repository.PipelineConfigRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/pipeline-config")
class PipelineConfigController(
    private val configRepository: PipelineConfigRepository,
    private val stepRegistry: ConfigurablePipelineStepRegistry
) {

    @GetMapping
    fun getAllConfigurations(): List<UseCasePipelineConfig> {
        return configRepository.findAll()
    }

    @GetMapping("/{useCase}")
    fun getConfiguration(@PathVariable useCase: String): ResponseEntity<UseCasePipelineConfig> {
        val config = configRepository.findByUseCase(useCase)
        return if (config != null) {
            ResponseEntity.ok(config)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    fun createConfiguration(@RequestBody request: CreatePipelineConfigRequest): ResponseEntity<UseCasePipelineConfig> {
        val config = UseCasePipelineConfig(
            useCase = request.useCase,
            description = request.description,
            steps = request.steps
        )

        val saved = configRepository.save(config)
        return ResponseEntity.ok(saved)
    }

    @PutMapping("/{useCase}")
    fun updateConfiguration(
        @PathVariable useCase: String,
        @RequestBody request: UpdatePipelineConfigRequest
    ): ResponseEntity<UseCasePipelineConfig> {
        val existingConfig = configRepository.findByUseCase(useCase)
            ?: return ResponseEntity.notFound().build()

        val updated = existingConfig.copy(
            description = request.description ?: existingConfig.description,
            steps = request.steps ?: existingConfig.steps,
            isActive = request.isActive ?: existingConfig.isActive
        )

        val saved = configRepository.save(updated)
        return ResponseEntity.ok(saved)
    }

    @DeleteMapping("/{useCase}")
    fun deleteConfiguration(@PathVariable useCase: String): ResponseEntity<Void> {
        return if (configRepository.deleteByUseCase(useCase)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/available-steps")
    fun getAvailableSteps(): List<String> {
        return stepRegistry.getAvailableStepNames()
    }
}

data class CreatePipelineConfigRequest(
    val useCase: String,
    val description: String,
    val steps: List<PipelineStepConfig>
)

data class UpdatePipelineConfigRequest(
    val description: String? = null,
    val steps: List<PipelineStepConfig>? = null,
    val isActive: Boolean? = null
)