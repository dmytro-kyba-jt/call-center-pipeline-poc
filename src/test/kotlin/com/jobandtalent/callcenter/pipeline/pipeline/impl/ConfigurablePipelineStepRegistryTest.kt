package com.jobandtalent.callcenter.pipeline.pipeline.impl

import com.jobandtalent.callcenter.pipeline.domain.CommunicationRequest
import com.jobandtalent.callcenter.pipeline.domain.PipelineResult
import com.jobandtalent.callcenter.pipeline.domain.PipelineStepConfig
import com.jobandtalent.callcenter.pipeline.domain.UseCasePipelineConfig
import com.jobandtalent.callcenter.pipeline.pipeline.PipelineStep
import com.jobandtalent.callcenter.pipeline.pipeline.steps.ConfigurableStepWrapper
import com.jobandtalent.callcenter.pipeline.repository.PipelineConfigRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.core.annotation.Order
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class ConfigurablePipelineStepRegistryTest {

    @Mock
    private lateinit var configRepository: PipelineConfigRepository

    private lateinit var registry: ConfigurablePipelineStepRegistry

    // Use real test step implementations instead of mocks
    private val step1 = TestStep1()
    private val step2 = TestStep2()
    private val step3 = TestStep3()

    @BeforeEach
    fun setup() {
        registry = ConfigurablePipelineStepRegistry(configRepository)
    }

    @Test
    fun `should register step successfully`() {
        registry.registerStep(step1)

        assertEquals(step1, registry.getStep("step1"))
    }

    @Test
    fun `should replace existing step when registering with same name`() {
        val newStep1 = TestPipelineStep("step1", "New Step 1 Description")

        registry.registerStep(step1)
        registry.registerStep(newStep1)

        val retrievedStep = registry.getStep("step1")
        assertEquals(newStep1, retrievedStep)
        assertEquals("New Step 1 Description", retrievedStep?.description)
    }

    @Test
    fun `should return steps ordered by Order annotation when no config exists`() {
        registry.registerStep(step1) // order 2
        registry.registerStep(step2) // order 1
        registry.registerStep(step3) // order 3

        val orderedSteps = registry.getOrderedSteps()

        assertEquals(3, orderedSteps.size)
        assertEquals("step1", orderedSteps[0].stepName) // order 1
        assertEquals("step2", orderedSteps[1].stepName) // order 2
        assertEquals("step3", orderedSteps[2].stepName) // order 3
    }

    @Test
    fun `should handle steps without Order annotation at end`() {
        val stepWithoutOrder = TestStepNoOrder()

        registry.registerStep(step2) // order 2
        registry.registerStep(stepWithoutOrder) // no order (Int.MAX_VALUE)

        val orderedSteps = registry.getOrderedSteps()

        assertEquals(2, orderedSteps.size)
        assertEquals("step2", orderedSteps[0].stepName)
        assertEquals("stepNoOrder", orderedSteps[1].stepName)
    }

    @Test
    fun `should return configured steps for use case in correct order`() {
        val useCase = "test-use-case"
        val config = UseCasePipelineConfig(
            useCase = useCase,
            description = "Test config",
            steps = listOf(
                PipelineStepConfig("step2", 1, true),
                PipelineStepConfig("step1", 2, true),
                PipelineStepConfig("step3", 3, false) // disabled
            )
        )

        whenever(configRepository.findByUseCase(useCase)).thenReturn(config)

        registry.registerStep(step1)
        registry.registerStep(step2)
        registry.registerStep(step3)

        val orderedSteps = registry.getOrderedStepsForUseCase(useCase)

        assertEquals(2, orderedSteps.size) // step3 is disabled
        assertEquals("step2", orderedSteps[0].stepName)
        assertEquals("step1", orderedSteps[1].stepName)
    }

    @Test
    fun `should wrap steps with configuration when present`() {
        val useCase = "test-use-case"
        val config = UseCasePipelineConfig(
            useCase = useCase,
            description = "Test config",
            steps = listOf(
                PipelineStepConfig(
                    stepName = "step1",
                    order = 1,
                    isEnabled = true,
                    configuration = mapOf("key" to "value"),
                    condition = "priority=HIGH"
                )
            )
        )

        whenever(configRepository.findByUseCase(useCase)).thenReturn(config)
        registry.registerStep(step1)

        val orderedSteps = registry.getOrderedStepsForUseCase(useCase)

        assertEquals(1, orderedSteps.size)
        assertTrue(orderedSteps[0] is ConfigurableStepWrapper)

        val wrapper = orderedSteps[0] as ConfigurableStepWrapper
        assertEquals("step1", wrapper.stepName)
    }

    @Test
    fun `should fallback to default ordering when config not found`() {
        val useCase = "non-existent-use-case"
        whenever(configRepository.findByUseCase(useCase)).thenReturn(null)

        registry.registerStep(step1)

        val orderedSteps = registry.getOrderedStepsForUseCase(useCase)

        assertEquals(1, orderedSteps.size)
        assertEquals(step1, orderedSteps[0])
    }

    @Test
    fun `should handle missing steps in configuration gracefully`() {
        val useCase = "test-use-case"
        val config = UseCasePipelineConfig(
            useCase = useCase,
            description = "Test config",
            steps = listOf(
                PipelineStepConfig("step1", 1, true),
                PipelineStepConfig("non-existent-step", 2, true)
            )
        )

        whenever(configRepository.findByUseCase(useCase)).thenReturn(config)
        registry.registerStep(step1)

        val orderedSteps = registry.getOrderedStepsForUseCase(useCase)

        assertEquals(1, orderedSteps.size) // Only step1 should be present
        assertEquals("step1", orderedSteps[0].stepName)
    }

    @Test
    fun `should return step by name`() {
        registry.registerStep(step1)

        assertEquals(step1, registry.getStep("step1"))
    }

    @Test
    fun `should return null for non-existent step`() {
        assertNull(registry.getStep("non-existent"))
    }

    @Test
    fun `should return all registered step names`() {
        registry.registerStep(step1)
        registry.registerStep(step2)

        val stepNames = registry.getAvailableStepNames()

        assertEquals(2, stepNames.size)
        assertTrue(stepNames.contains("step1"))
        assertTrue(stepNames.contains("step2"))
    }

    @Test
    fun `should return empty list when no steps registered`() {
        val stepNames = registry.getAvailableStepNames()

        assertTrue(stepNames.isEmpty())
    }

    // Simple test implementation of PipelineStep
    private class TestPipelineStep(
        private val name: String,
        private val desc: String
    ) : PipelineStep {

        override val stepName: String = name
        override val description: String = desc

        override fun canExecute(request: CommunicationRequest): Boolean = true
        override suspend fun execute(request: CommunicationRequest): PipelineResult =
            PipelineResult.success(request, "Test execution for $name")
    }
}

// Test step classes with specific order annotations
@Order(1)
private class TestStep1 : PipelineStep {
    override val stepName = "step1"
    override val description = "Step 1 Description"
    override fun canExecute(request: CommunicationRequest) = true
    override suspend fun execute(request: CommunicationRequest) =
        PipelineResult.success(request, "Test execution for step1")
}

@Order(2)
private class TestStep2 : PipelineStep {
    override val stepName = "step2"
    override val description = "Step 2 Description"
    override fun canExecute(request: CommunicationRequest) = true
    override suspend fun execute(request: CommunicationRequest) =
        PipelineResult.success(request, "Test execution for step2")
}

@Order(3)
private class TestStep3 : PipelineStep {
    override val stepName = "step3"
    override val description = "Step 3 Description"
    override fun canExecute(request: CommunicationRequest) = true
    override suspend fun execute(request: CommunicationRequest) =
        PipelineResult.success(request, "Test execution for step3")
}

private class TestStepNoOrder : PipelineStep {
    override val stepName = "stepNoOrder"
    override val description = "No Order Step"
    override fun canExecute(request: CommunicationRequest) = true
    override suspend fun execute(request: CommunicationRequest) =
        PipelineResult.success(request, "Test execution for stepNoOrder")
}