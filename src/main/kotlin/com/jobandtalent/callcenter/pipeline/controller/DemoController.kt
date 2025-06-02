package com.jobandtalent.callcenter.pipeline.controller

import com.jobandtalent.callcenter.pipeline.domain.*
import com.jobandtalent.callcenter.pipeline.repository.PipelineConfigRepository
import com.jobandtalent.callcenter.pipeline.service.CommunicationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/demo")
class DemoController(
    private val communicationService: CommunicationService
) {
    @Autowired
    private lateinit var configRepository: PipelineConfigRepository


    @PostMapping("/setup-live-demo")
    fun setupLiveDemo(): ResponseEntity<Map<String, Any>> {
        // Delete any existing configuration first
        configRepository.deleteByUseCase("live-config-demo")

        // Create initial configuration for live demo
        val initialConfig = UseCasePipelineConfig(
            useCase = "live-config-demo",
            description = "🔄 Initial configuration - Full pipeline with all steps",
            steps = listOf(
                PipelineStepConfig("acceptanceRules", 1, true),
                PipelineStepConfig("dataStorage", 2, true),
                PipelineStepConfig("exclusionRules", 3, true),
                PipelineStepConfig("scheduler", 4, true, configuration = mapOf(
                    "delay" to 100,
                    "retries" to 2
                )),
                PipelineStepConfig("communicationProvider", 5, true)
            )
        )

        configRepository.save(initialConfig)

        return ResponseEntity.ok(mapOf(
            "message" to "Live demo configuration created successfully",
            "useCase" to "live-config-demo",
            "steps" to initialConfig.steps.map { "${it.stepName} (order: ${it.order})" },
            "totalSteps" to initialConfig.steps.size,
            "configId" to initialConfig.id
        ))
    }

    @PostMapping("/modify-live-demo")
    fun modifyLiveDemo(): ResponseEntity<Map<String, Any>> {
        val existingConfig = configRepository.findByUseCase("live-config-demo")
            ?: return ResponseEntity.badRequest().body(mapOf(
                "error" to "Live demo configuration not found. Please run setup first."
            ))

        // Create modified configuration - keeping the same ID to overwrite
        val modifiedConfig = existingConfig.copy(
            description = "⚡ Modified configuration - Streamlined for speed",
            steps = listOf(
                PipelineStepConfig("acceptanceRules", 1, true),
                // Skip dataStorage and exclusionRules for speed
                PipelineStepConfig("scheduler", 2, true,
                    condition = "priority=HIGH", // Only schedule if HIGH priority
                    configuration = mapOf(
                        "delay" to 50,  // Faster scheduling
                        "retries" to 1  // Fewer retries
                    )
                ),
                PipelineStepConfig("communicationProvider", 3, true)
            ),
            updatedAt = java.time.LocalDateTime.now()
        )

        configRepository.save(modifiedConfig)

        return ResponseEntity.ok(mapOf(
            "message" to "Live demo configuration modified successfully",
            "useCase" to "live-config-demo",
            "changes" to listOf(
                "❌ Removed: dataStorage step",
                "❌ Removed: exclusionRules step",
                "⚡ Modified: scheduler now conditional (priority=HIGH)",
                "⚡ Modified: faster scheduling (50ms vs 100ms)",
                "⚡ Modified: fewer retries (1 vs 2)"
            ),
            "oldSteps" to existingConfig.steps.size,
            "newSteps" to modifiedConfig.steps.size,
            "stepsRemoved" to (existingConfig.steps.size - modifiedConfig.steps.size),
            "configId" to modifiedConfig.id
        ))
    }

    @PostMapping("/restore-live-demo")
    fun restoreLiveDemo(): ResponseEntity<Map<String, Any>> {
        val existingConfig = configRepository.findByUseCase("live-config-demo")
            ?: return ResponseEntity.badRequest().body(mapOf(
                "error" to "Live demo configuration not found. Please run setup first."
            ))

        // Restore to original configuration - keeping the same ID to overwrite
        val restoredConfig = existingConfig.copy(
            description = "🔄 Restored configuration - Back to full pipeline",
            steps = listOf(
                PipelineStepConfig("acceptanceRules", 1, true),
                PipelineStepConfig("dataStorage", 2, true),
                PipelineStepConfig("exclusionRules", 3, true),
                PipelineStepConfig("scheduler", 4, true, configuration = mapOf(
                    "delay" to 100,
                    "retries" to 2
                )),
                PipelineStepConfig("communicationProvider", 5, true)
            ),
            updatedAt = java.time.LocalDateTime.now()
        )

        configRepository.save(restoredConfig)

        return ResponseEntity.ok(mapOf(
            "message" to "Live demo configuration restored to original successfully",
            "useCase" to "live-config-demo",
            "steps" to restoredConfig.steps.map { "${it.stepName} (order: ${it.order})" },
            "totalSteps" to restoredConfig.steps.size,
            "changes" to listOf(
                "✅ Restored: dataStorage step",
                "✅ Restored: exclusionRules step",
                "🔄 Restored: scheduler back to unconditional",
                "🔄 Restored: original timing (100ms delay)",
                "🔄 Restored: original retries (2)"
            ),
            "configId" to restoredConfig.id
        ))
    }

    @GetMapping("/live-demo-status")
    fun getLiveDemoStatus(): ResponseEntity<Map<String, Any>> {
        val config = configRepository.findByUseCase("live-config-demo")
            ?: return ResponseEntity.ok(mapOf(
                "exists" to false,
                "message" to "Live demo configuration not found. Run setup first."
            ))

        return ResponseEntity.ok(mapOf(
            "exists" to true,
            "useCase" to config.useCase,
            "description" to config.description,
            "steps" to config.steps.map { step ->
                mapOf(
                    "name" to step.stepName,
                    "order" to step.order,
                    "enabled" to step.isEnabled,
                    "condition" to step.condition,
                    "configuration" to step.configuration
                )
            },
            "totalSteps" to config.steps.size,
            "lastUpdated" to config.updatedAt.toString(),
            "configId" to config.id
        ))
    }

    @GetMapping("/debug/all-configs")
    fun getAllConfigs(): ResponseEntity<List<UseCasePipelineConfig>> {
        return ResponseEntity.ok(configRepository.findAll())
    }

    @DeleteMapping("/debug/clear-live-demo")
    fun clearLiveDemo(): ResponseEntity<Map<String, Any>> {
        val deleted = configRepository.deleteByUseCase("live-config-demo")
        return ResponseEntity.ok(mapOf(
            "message" to "Live demo configuration cleared",
            "deleted" to deleted
        ))
    }

    @GetMapping("/scenarios")
    fun getDemoScenarios(): List<DemoScenario> {
        return listOf(
            DemoScenario(
                id = "live-config-demo",
                name = "🔄 Live Configuration Demo",
                description = "Demonstrates runtime pipeline modification - run, modify, run again!",
                useCase = "live-config-demo",
                priority = Priority.NORMAL,
                communicationType = CommunicationType.EMAIL,
                expectedSteps = listOf("acceptanceRules", "dataStorage", "scheduler", "communicationProvider"),
                estimatedTime = "~400ms"
            ),
            DemoScenario(
                id = "emergency",
                name = "⚡ Emergency Alert",
                description = "Ultra-fast pipeline bypassing storage and exclusions",
                useCase = "emergency-alert",
                priority = Priority.URGENT,
                communicationType = CommunicationType.CALL,
                expectedSteps = listOf("acceptanceRules", "communicationProvider"),
                estimatedTime = "~300ms"
            ),
            DemoScenario(
                id = "marketing",
                name = "📈 Marketing Campaign",
                description = "Full compliance pipeline with all safeguards",
                useCase = "marketing-campaign",
                priority = Priority.NORMAL,
                communicationType = CommunicationType.EMAIL,
                expectedSteps = listOf("acceptanceRules", "dataStorage", "exclusionRules", "scheduler", "communicationProvider"),
                estimatedTime = "~500ms"
            ),
            DemoScenario(
                id = "priority-high",
                name = "🔥 High Priority Notification",
                description = "Conditional pipeline - skips storage/exclusions for HIGH priority",
                useCase = "priority-notification",
                priority = Priority.HIGH,
                communicationType = CommunicationType.SMS,
                expectedSteps = listOf("acceptanceRules", "scheduler", "communicationProvider"),
                estimatedTime = "~350ms"
            ),
            DemoScenario(
                id = "priority-normal",
                name = "📝 Normal Priority Notification",
                description = "Conditional pipeline - includes all steps for NORMAL priority",
                useCase = "priority-notification",
                priority = Priority.NORMAL,
                communicationType = CommunicationType.EMAIL,
                expectedSteps = listOf("acceptanceRules", "dataStorage", "exclusionRules", "scheduler", "communicationProvider"),
                estimatedTime = "~500ms"
            ),
            DemoScenario(
                id = "blocked-worker",
                name = "🚫 Blocked Worker Demo",
                description = "Shows exclusion rules in action",
                useCase = "marketing-campaign",
                priority = Priority.NORMAL,
                communicationType = CommunicationType.EMAIL,
                workerId = "blocked-worker-1",
                expectedSteps = listOf("acceptanceRules", "dataStorage", "exclusionRules"),
                estimatedTime = "~300ms",
                expectedResult = "FAILED"
            ),
            DemoScenario(
                id = "default-fallback",
                name = "🔄 Default Pipeline Fallback",
                description = "Uses annotation-based ordering when no config exists",
                useCase = "unknown-case",
                priority = Priority.NORMAL,
                communicationType = CommunicationType.CALL,
                expectedSteps = listOf("acceptanceRules", "dataStorage", "scheduler", "exclusionRules", "communicationProvider"),
                estimatedTime = "~600ms"
            )
        )
    }

    @PostMapping("/run/{scenarioId}")
    suspend fun runDemoScenario(@PathVariable scenarioId: String): ResponseEntity<DemoResult> {
        val scenario = getDemoScenarios().find { it.id == scenarioId }
            ?: return ResponseEntity.notFound().build()

        val startTime = System.currentTimeMillis()

        val request = CommunicationRequest(
            workerId = scenario.workerId ?: "demo-worker-${System.currentTimeMillis()}",
            communicationType = scenario.communicationType,
            payload = generatePayload(scenario.communicationType),
            useCase = scenario.useCase,
            priority = scenario.priority
        )

        val result = communicationService.processCommunicationRequest(request)
        val duration = System.currentTimeMillis() - startTime

        val demoResult = DemoResult(
            scenario = scenario,
            request = request,
            result = result,
            actualDuration = "${duration}ms",
            executedSteps = extractExecutedSteps(result),
            metadata = result.request.metadata
        )

        return ResponseEntity.ok(demoResult)
    }

    @PostMapping("/batch-demo")
    suspend fun runBatchDemo(): ResponseEntity<BatchDemoResult> {
        val scenarios = listOf("emergency", "marketing", "priority-high", "blocked-worker")
        val results = mutableListOf<DemoResult>()
        val startTime = System.currentTimeMillis()

        for (scenarioId in scenarios) {
            val response = runDemoScenario(scenarioId)
            if (response.statusCode.is2xxSuccessful) {
                results.add(response.body!!)
            }
        }

        val totalDuration = System.currentTimeMillis() - startTime

        return ResponseEntity.ok(
            BatchDemoResult(
                totalScenarios = scenarios.size,
                successfulScenarios = results.count { it.result.success },
                failedScenarios = results.count { !it.result.success },
                totalDuration = "${totalDuration}ms",
                results = results
            )
        )
    }

    private fun generatePayload(communicationType: CommunicationType): Map<String, Any> {
        return when (communicationType) {
            CommunicationType.CALL -> mapOf(
                "phoneNumber" to "+1-555-DEMO",
                "script" to "This is a demo call from the Communication Pipeline system."
            )
            CommunicationType.SMS -> mapOf(
                "phoneNumber" to "+1-555-DEMO",
                "message" to "Demo SMS: Pipeline processing complete! ✅"
            )
            CommunicationType.EMAIL -> mapOf(
                "to" to "demo@example.com",
                "subject" to "Demo: Communication Pipeline Test",
                "body" to "This is a demonstration email from the configurable communication pipeline."
            )
            CommunicationType.PUSH_NOTIFICATION -> mapOf(
                "title" to "Demo Notification",
                "body" to "Pipeline demo notification",
                "badge" to 1
            )
        }
    }

    private fun extractExecutedSteps(result: PipelineResult): List<String> {
        val statusHistory = result.request.metadata["statusHistory"] as? List<String> ?: emptyList()
        return statusHistory.mapNotNull { entry ->
            when {
                entry.contains("ACCEPTANCE_RULES") -> "acceptanceRules"
                entry.contains("DATA_STORED") -> "dataStorage"
                entry.contains("SCHEDULED") -> "scheduler"
                entry.contains("EXCLUSION_RULES") -> "exclusionRules"
                entry.contains("SENT_TO_PROVIDER") -> "communicationProvider"
                else -> null
            }
        }.distinct()
    }
}

data class DemoScenario(
    val id: String,
    val name: String,
    val description: String,
    val useCase: String,
    val priority: Priority,
    val communicationType: CommunicationType,
    val workerId: String? = null,
    val expectedSteps: List<String>,
    val estimatedTime: String,
    val expectedResult: String = "SUCCESS"
)

data class DemoResult(
    val scenario: DemoScenario,
    val request: CommunicationRequest,
    val result: PipelineResult,
    val actualDuration: String,
    val executedSteps: List<String>,
    val metadata: Map<String, Any>
)

data class BatchDemoResult(
    val totalScenarios: Int,
    val successfulScenarios: Int,
    val failedScenarios: Int,
    val totalDuration: String,
    val results: List<DemoResult>
)