package com.jobandtalent.callcenter.pipeline.config

import com.jobandtalent.callcenter.pipeline.domain.PipelineStepConfig
import com.jobandtalent.callcenter.pipeline.domain.UseCasePipelineConfig
import com.jobandtalent.callcenter.pipeline.repository.PipelineConfigRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order

@Configuration
class DemoDataConfiguration {

    @Bean
    @Order(2) // Run after PipelineConfiguration
    fun demoDataInitializer(configRepository: PipelineConfigRepository): CommandLineRunner {
        return CommandLineRunner {
            println("\n🚀 Initializing Demo Pipeline Configurations...")

            // 1. Emergency Alert Pipeline (Ultra Fast)
            val emergencyConfig = UseCasePipelineConfig(
                useCase = "emergency-alert",
                description = "⚡ Ultra-fast pipeline for emergency notifications - bypasses storage and exclusions",
                steps = listOf(
                    PipelineStepConfig("acceptanceRules", 1, true),
                    PipelineStepConfig("communicationProvider", 2, true)
                )
            )

            // 2. Marketing Campaign Pipeline (Full Compliance)
            val marketingConfig = UseCasePipelineConfig(
                useCase = "marketing-campaign",
                description = "📈 Full compliance pipeline with all safeguards for marketing",
                steps = listOf(
                    PipelineStepConfig("acceptanceRules", 1, true),
                    PipelineStepConfig("dataStorage", 2, true),
                    PipelineStepConfig("exclusionRules", 3, true),
                    PipelineStepConfig("scheduler", 4, true, configuration = mapOf(
                        "batchSize" to 1000,
                        "maxRetries" to 3,
                        "delayBetweenBatches" to 30
                    )),
                    PipelineStepConfig("communicationProvider", 5, true)
                )
            )

            // 3. Priority-Based Conditional Pipeline
            val conditionalConfig = UseCasePipelineConfig(
                useCase = "priority-notification",
                description = "🎯 Smart pipeline with conditional execution based on priority",
                steps = listOf(
                    PipelineStepConfig("acceptanceRules", 1, true),
                    PipelineStepConfig("dataStorage", 2, true, condition = "priority=NORMAL"),
                    PipelineStepConfig("exclusionRules", 3, true, condition = "priority=NORMAL"),
                    PipelineStepConfig("scheduler", 4, true, configuration = mapOf(
                        "urgentDelay" to 0,
                        "normalDelay" to 15
                    )),
                    PipelineStepConfig("communicationProvider", 5, true)
                )
            )

            // 4. SMS-Only Pipeline
            val smsOnlyConfig = UseCasePipelineConfig(
                useCase = "sms-notification",
                description = "📱 Specialized pipeline for SMS communications only",
                steps = listOf(
                    PipelineStepConfig("acceptanceRules", 1, true),
                    PipelineStepConfig("exclusionRules", 2, true, condition = "communicationType=SMS"),
                    PipelineStepConfig("scheduler", 3, true, configuration = mapOf(
                        "rateLimitPerMinute" to 60
                    )),
                    PipelineStepConfig("communicationProvider", 4, true)
                )
            )

            // 5. Bulk Communication Pipeline
            val bulkConfig = UseCasePipelineConfig(
                useCase = "bulk-notification",
                description = "📊 High-volume pipeline optimized for bulk communications",
                steps = listOf(
                    PipelineStepConfig("acceptanceRules", 1, true),
                    PipelineStepConfig("dataStorage", 2, true),
                    PipelineStepConfig("scheduler", 3, true, configuration = mapOf(
                        "batchSize" to 5000,
                        "parallelProcessing" to true,
                        "maxConcurrency" to 10
                    )),
                    PipelineStepConfig("communicationProvider", 4, true)
                )
            )

            // Save all configurations
            val configs = listOf(emergencyConfig, marketingConfig, conditionalConfig, smsOnlyConfig, bulkConfig)
            configs.forEach { config ->
                configRepository.save(config)
                println("✅ Created pipeline config: ${config.useCase} - ${config.description}")
            }

            println("\n📋 Demo Pipeline Configurations Ready!")
            println("Available use cases:")
            configs.forEach { config ->
                println("  • ${config.useCase}: ${config.steps.size} steps")
            }

            println("\n🎯 Demo is ready! Try these use cases with different priorities and communication types.")
        }
    }
}