package com.jobandtalent.callcenter.pipeline.pipeline.impl

import com.jobandtalent.callcenter.pipeline.pipeline.PipelineStep
import com.jobandtalent.callcenter.pipeline.pipeline.PipelineStepRegistry
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
class PipelineStepRegistryImpl : PipelineStepRegistry {
    private val steps = mutableMapOf<String, PipelineStep>()

    override fun registerStep(step: PipelineStep) {
        steps[step.stepName] = step
    }

    override fun getOrderedSteps(): List<PipelineStep> {
        return steps.values.sortedBy { step ->
            AnnotationUtils.findAnnotation(step.javaClass, Order::class.java)?.value ?: Int.MAX_VALUE
        }
    }

    override fun getStep(stepName: String): PipelineStep? = steps[stepName]
}
