package com.jobandtalent.callcenter.pipeline.pipeline.impl

import com.jobandtalent.callcenter.pipeline.pipeline.PipelineStep
import com.jobandtalent.callcenter.pipeline.pipeline.PipelineStepRegistry
import com.jobandtalent.callcenter.pipeline.pipeline.steps.ConfigurableStepWrapper
import com.jobandtalent.callcenter.pipeline.repository.PipelineConfigRepository
import org.springframework.context.annotation.Primary
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Primary
class ConfigurablePipelineStepRegistry(
    private val configRepository: PipelineConfigRepository
) : PipelineStepRegistry {

    private val availableSteps = mutableMapOf<String, PipelineStep>()

    override fun registerStep(step: PipelineStep) {
        availableSteps[step.stepName] = step
    }

    override fun getOrderedSteps(): List<PipelineStep> {
        // Return all available steps with default order if no specific config
        return availableSteps.values.sortedBy { step ->
            AnnotationUtils.findAnnotation(step.javaClass, Order::class.java)?.value ?: Int.MAX_VALUE
        }
    }

    // New method for use case specific ordering
    fun getOrderedStepsForUseCase(useCase: String): List<PipelineStep> {
        val config = configRepository.findByUseCase(useCase)

        return if (config != null) {
            // Use database configuration
            config.steps
                .filter { it.isEnabled }
                .sortedBy { it.order }
                .mapNotNull { stepConfig ->
                    availableSteps[stepConfig.stepName]?.let { step ->
                        // Wrap step with configuration if needed
                        if (stepConfig.configuration.isNotEmpty()) {
                            ConfigurableStepWrapper(step, stepConfig.configuration, stepConfig.condition)
                        } else {
                            step
                        }
                    }
                }
        } else {
            // Fallback to annotation-based ordering
            getOrderedSteps()
        }
    }

    override fun getStep(stepName: String): PipelineStep? = availableSteps[stepName]

    fun getAvailableStepNames(): List<String> = availableSteps.keys.toList()
}