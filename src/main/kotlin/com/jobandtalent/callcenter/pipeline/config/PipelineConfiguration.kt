package com.jobandtalent.callcenter.pipeline.config

import com.jobandtalent.callcenter.pipeline.pipeline.PipelineStep
import com.jobandtalent.callcenter.pipeline.pipeline.PipelineStepRegistry
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PipelineConfiguration {

    @Bean
    fun pipelineInitializer(
        stepRegistry: PipelineStepRegistry,
        pipelineSteps: List<PipelineStep>
    ): CommandLineRunner {
        return CommandLineRunner {
            pipelineSteps.forEach { step ->
                stepRegistry.registerStep(step)
                println("Registered pipeline step: ${step.stepName} - ${step.description}")
            }

            println("\nPipeline execution order:")
            stepRegistry.getOrderedSteps().forEachIndexed { index, step ->
                println("${index + 1}. ${step.stepName} - ${step.description}")
            }
        }
    }
}
