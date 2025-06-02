package com.jobandtalent.callcenter.pipeline.steps

import com.jobandtalent.callcenter.pipeline.domain.CommunicationRequest
import com.jobandtalent.callcenter.pipeline.domain.CommunicationType
import com.jobandtalent.callcenter.pipeline.domain.PipelineResult
import com.jobandtalent.callcenter.pipeline.domain.Priority
import com.jobandtalent.callcenter.pipeline.pipeline.PipelineStep
import com.jobandtalent.callcenter.pipeline.pipeline.steps.ConfigurableStepWrapper
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class ConfigurableStepWrapperTest {

    @Mock
    private lateinit var delegateStep: PipelineStep

    private lateinit var testRequest: CommunicationRequest

    @BeforeEach
    fun setup() {
        testRequest = CommunicationRequest(
            id = "test-id",
            workerId = "worker-1",
            communicationType = CommunicationType.EMAIL,
            payload = mapOf("test" to "data"),
            useCase = "test-case",
            priority = Priority.NORMAL,
            metadata = mutableMapOf()
        )
    }

    @Nested
    inner class ExecuteTests {

        @Test
        fun `should execute delegate with enhanced request containing configuration`() = runTest {
            // Given
            val configuration = mapOf("key1" to "value1", "key2" to 42)
            val wrapper = ConfigurableStepWrapper(delegateStep, configuration, null)

            val expectedResult = PipelineResult.success(testRequest, "Success")
            whenever(delegateStep.execute(any())).thenReturn(expectedResult)

            // When
            val result = wrapper.execute(testRequest)

            // Then
            verify(delegateStep).execute(argThat { request ->
                request.metadata["stepConfiguration"] == configuration &&
                        request.id == testRequest.id &&
                        request.workerId == testRequest.workerId
            })
            assertEquals(expectedResult, result)
        }

        @Test
        fun `should preserve original request metadata while adding configuration`() = runTest {
            // Given
            val originalMetadata: MutableMap<String, Any> = mutableMapOf("original" to "data", "existing" to "value")
            val requestWithMetadata = testRequest.copy(metadata = originalMetadata)
            val configuration = mapOf("config" to "test")

            val wrapper = ConfigurableStepWrapper(delegateStep, configuration, null)
            val expectedResult = PipelineResult.success(requestWithMetadata, "Success")
            whenever(delegateStep.execute(any())).thenReturn(expectedResult)

            // When
            val result = wrapper.execute(requestWithMetadata)

            // Then
            verify(delegateStep).execute(argThat { request ->
                request.metadata["original"] == "data" &&
                        request.metadata["existing"] == "value" &&
                        request.metadata["stepConfiguration"] == configuration
            })
            assertEquals(expectedResult, result)
        }

        @Test
        fun `should handle empty configuration`() = runTest {
            // Given
            val wrapper = ConfigurableStepWrapper(delegateStep, emptyMap(), null)
            val expectedResult = PipelineResult.success(testRequest, "Success")
            whenever(delegateStep.execute(any())).thenReturn(expectedResult)

            // When
            val result = wrapper.execute(testRequest)

            // Then
            verify(delegateStep).execute(argThat { request ->
                request.metadata["stepConfiguration"] == emptyMap<String, Any>()
            })
            assertEquals(expectedResult, result)
        }

        @Test
        fun `should handle complex configuration objects`() = runTest {
            // Given
            val configuration = mapOf(
                "stringValue" to "test",
                "intValue" to 42,
                "booleanValue" to true,
                "listValue" to listOf("a", "b", "c"),
                "mapValue" to mapOf("nested" to "value")
            )
            val wrapper = ConfigurableStepWrapper(delegateStep, configuration, null)

            val expectedResult = PipelineResult.success(testRequest, "Success")
            whenever(delegateStep.execute(any())).thenReturn(expectedResult)

            // When
            val result = wrapper.execute(testRequest)

            // Then
            verify(delegateStep).execute(argThat { request ->
                val stepConfig = request.metadata["stepConfiguration"] as Map<*, *>
                stepConfig["stringValue"] == "test" &&
                        stepConfig["intValue"] == 42 &&
                        stepConfig["booleanValue"] == true &&
                        stepConfig["listValue"] == listOf("a", "b", "c") &&
                        stepConfig["mapValue"] == mapOf("nested" to "value")
            })
            assertEquals(expectedResult, result)
        }
    }

    @Nested
    inner class CanExecuteTests {

        @Test
        fun `should return true when no condition and delegate can execute`() {
            // Given
            val wrapper = ConfigurableStepWrapper(delegateStep, emptyMap(), null)
            whenever(delegateStep.canExecute(testRequest)).thenReturn(true)

            // When
            val canExecute = wrapper.canExecute(testRequest)

            // Then
            assertTrue(canExecute)
            verify(delegateStep).canExecute(testRequest)
        }

        @Test
        fun `should return false when delegate cannot execute regardless of condition`() {
            // Given
            val wrapper = ConfigurableStepWrapper(delegateStep, emptyMap(), "priority=NORMAL")
            whenever(delegateStep.canExecute(testRequest)).thenReturn(false)

            // When
            val canExecute = wrapper.canExecute(testRequest)

            // Then
            assertFalse(canExecute)
            verify(delegateStep).canExecute(testRequest)
        }

        @Nested
        inner class PriorityConditionTests {

            @Test
            fun `should evaluate priority condition correctly for HIGH priority`() {
                // Given
                val wrapper = ConfigurableStepWrapper(delegateStep, emptyMap(), "priority=HIGH")
                whenever(delegateStep.canExecute(any())).thenReturn(true)

                val highPriorityRequest = testRequest.copy(priority = Priority.HIGH)
                val normalPriorityRequest = testRequest.copy(priority = Priority.NORMAL)

                // When & Then
                assertTrue(wrapper.canExecute(highPriorityRequest))
                assertFalse(wrapper.canExecute(normalPriorityRequest))
            }

            @Test
            fun `should evaluate priority condition case insensitively`() {
                // Given
                val wrapper = ConfigurableStepWrapper(delegateStep, emptyMap(), "priority=high")
                whenever(delegateStep.canExecute(any())).thenReturn(true)

                val highPriorityRequest = testRequest.copy(priority = Priority.HIGH)

                // When & Then
                assertTrue(wrapper.canExecute(highPriorityRequest))
            }

            @Test
            fun `should handle all priority levels`() {
                // Given
                val priorities = listOf(
                    Priority.LOW to "priority=LOW",
                    Priority.NORMAL to "priority=NORMAL",
                    Priority.HIGH to "priority=HIGH",
                    Priority.URGENT to "priority=URGENT"
                )

                priorities.forEach { (priority, condition) ->
                    val wrapper = ConfigurableStepWrapper(delegateStep, emptyMap(), condition)
                    whenever(delegateStep.canExecute(any())).thenReturn(true)

                    val request = testRequest.copy(priority = priority)

                    // When & Then
                    assertTrue(wrapper.canExecute(request), "Should execute for $priority")

                    // Reset mock for next iteration
                    reset(delegateStep)
                }
            }
        }

        @Nested
        inner class CommunicationTypeConditionTests {

            @Test
            fun `should evaluate communication type condition correctly`() {
                // Given
                val wrapper = ConfigurableStepWrapper(delegateStep, emptyMap(), "communicationType=SMS")
                whenever(delegateStep.canExecute(any())).thenReturn(true)

                val smsRequest = testRequest.copy(communicationType = CommunicationType.SMS)
                val emailRequest = testRequest.copy(communicationType = CommunicationType.EMAIL)

                // When & Then
                assertTrue(wrapper.canExecute(smsRequest))
                assertFalse(wrapper.canExecute(emailRequest))
            }

            @Test
            fun `should evaluate communication type condition case insensitively`() {
                // Given
                val wrapper = ConfigurableStepWrapper(delegateStep, emptyMap(), "communicationType=sms")
                whenever(delegateStep.canExecute(any())).thenReturn(true)

                val smsRequest = testRequest.copy(communicationType = CommunicationType.SMS)

                // When & Then
                assertTrue(wrapper.canExecute(smsRequest))
            }

            @Test
            fun `should handle all communication types`() {
                // Given
                val communicationTypes = listOf(
                    CommunicationType.CALL to "communicationType=CALL",
                    CommunicationType.SMS to "communicationType=SMS",
                    CommunicationType.EMAIL to "communicationType=EMAIL",
                    CommunicationType.PUSH_NOTIFICATION to "communicationType=PUSH_NOTIFICATION"
                )

                communicationTypes.forEach { (type, condition) ->
                    val wrapper = ConfigurableStepWrapper(delegateStep, emptyMap(), condition)
                    whenever(delegateStep.canExecute(any())).thenReturn(true)

                    val request = testRequest.copy(communicationType = type)

                    // When & Then
                    assertTrue(wrapper.canExecute(request), "Should execute for $type")

                    // Reset mock for next iteration
                    reset(delegateStep)
                }
            }
        }

        @Test
        fun `should return false when condition fails and not check delegate`() {
            // Given
            val wrapper = ConfigurableStepWrapper(delegateStep, emptyMap(), "priority=HIGH")
            // Don't stub canExecute since it shouldn't be called

            val normalPriorityRequest = testRequest.copy(priority = Priority.NORMAL)

            // When
            val canExecute = wrapper.canExecute(normalPriorityRequest)

            // Then
            assertFalse(canExecute)
            // Should not check delegate when condition fails
            verify(delegateStep, never()).canExecute(any())
        }

        @Test
        fun `should return true for unknown condition types as fallback`() {
            // Given
            val wrapper = ConfigurableStepWrapper(delegateStep, emptyMap(), "unknownCondition=value")
            whenever(delegateStep.canExecute(testRequest)).thenReturn(true)

            // When
            val canExecute = wrapper.canExecute(testRequest)

            // Then
            assertTrue(canExecute)
            verify(delegateStep).canExecute(testRequest)
        }

        @Test
        fun `should handle malformed conditions as unknown condition type`() {
            // Given - these should all be treated as unknown conditions (return true + check delegate)
            val malformedConditions = listOf(
                "priority", // Missing equals
                "=HIGH",    // Missing key
                "",         // Empty condition
                "priority=HIGH=EXTRA", // Multiple equals - treated as malformed
                "communicationType=SMS=EXTRA" // Multiple equals for communication type
            )

            malformedConditions.forEach { condition ->
                val wrapper = ConfigurableStepWrapper(delegateStep, emptyMap(), condition)
                whenever(delegateStep.canExecute(any())).thenReturn(true)

                // When & Then - should fallback to checking delegate
                assertTrue(wrapper.canExecute(testRequest), "Should handle malformed condition: '$condition'")

                // Reset mock for next iteration
                reset(delegateStep)
            }
        }

        @Test
        fun `should handle empty value in condition as no match`() {
            // Given - empty value after equals should not match any priority
            val wrapper = ConfigurableStepWrapper(delegateStep, emptyMap(), "priority=")
            // Don't stub canExecute since condition should fail

            // When
            val canExecute = wrapper.canExecute(testRequest)

            // Then - empty value should not match NORMAL priority
            assertFalse(canExecute)
            verify(delegateStep, never()).canExecute(any())
        }

        @Test
        fun `should handle specific malformed conditions with multiple equals`() {
            // Given
            val malformedConditions = mapOf(
                "priority=HIGH=EXTRA" to "Multiple equals in priority condition",
                "communicationType=SMS=INVALID" to "Multiple equals in communication type condition",
                "priority==HIGH" to "Double equals"
            )

            malformedConditions.forEach { (condition, description) ->
                val wrapper = ConfigurableStepWrapper(delegateStep, emptyMap(), condition)
                whenever(delegateStep.canExecute(any())).thenReturn(true)

                // When & Then - all should be treated as malformed and fallback to delegate
                assertTrue(wrapper.canExecute(testRequest), "Failed for: $description")
                verify(delegateStep).canExecute(testRequest)

                // Reset mock for next iteration
                reset(delegateStep)
            }
        }
    }

    @Nested
    inner class CallbackTests {

        @Test
        fun `should delegate onSuccess to wrapped step`() {
            // Given
            val wrapper = ConfigurableStepWrapper(delegateStep, emptyMap(), null)

            // When
            wrapper.onSuccess(testRequest)

            // Then
            verify(delegateStep).onSuccess(testRequest)
        }

        @Test
        fun `should delegate onFailure to wrapped step`() {
            // Given
            val wrapper = ConfigurableStepWrapper(delegateStep, emptyMap(), null)
            val error = RuntimeException("Test error")

            // When
            wrapper.onFailure(testRequest, error)

            // Then
            verify(delegateStep).onFailure(testRequest, error)
        }

        @Test
        fun `should delegate callbacks with different request instances`() {
            // Given
            val wrapper = ConfigurableStepWrapper(delegateStep, emptyMap(), null)
            val differentRequest = testRequest.copy(id = "different-id")
            val error = IllegalStateException("Different error")

            // When
            wrapper.onSuccess(differentRequest)
            wrapper.onFailure(testRequest, error)

            // Then
            verify(delegateStep).onSuccess(differentRequest)
            verify(delegateStep).onFailure(testRequest, error)
        }
    }

    @Nested
    inner class PropertyTests {

        @Test
        fun `should return delegate step name`() {
            // Given
            whenever(delegateStep.stepName).thenReturn("test-step")
            val wrapper = ConfigurableStepWrapper(delegateStep, emptyMap(), null)

            // When & Then
            assertEquals("test-step", wrapper.stepName)
        }

        @Test
        fun `should return delegate description`() {
            // Given
            whenever(delegateStep.description).thenReturn("Test Step Description")
            val wrapper = ConfigurableStepWrapper(delegateStep, emptyMap(), null)

            // When & Then
            assertEquals("Test Step Description", wrapper.description)
        }

        @Test
        fun `should return delegate properties consistently`() {
            // Given
            whenever(delegateStep.stepName).thenReturn("test-step")
            whenever(delegateStep.description).thenReturn("Test Step Description")
            val wrapper = ConfigurableStepWrapper(delegateStep, emptyMap(), null)

            // When - call multiple times
            val stepName1 = wrapper.stepName
            val stepName2 = wrapper.stepName
            val description1 = wrapper.description
            val description2 = wrapper.description

            // Then - should be consistent
            assertEquals(stepName1, stepName2)
            assertEquals(description1, description2)
            assertEquals("test-step", stepName1)
            assertEquals("Test Step Description", description1)
        }
    }

    @Nested
    inner class IntegrationTests {

        @Test
        fun `should work with complex condition and configuration together`() = runTest {
            // Given
            val configuration = mapOf("retryCount" to 3, "timeout" to 5000)
            val wrapper = ConfigurableStepWrapper(delegateStep, configuration, "priority=HIGH")

            val highPriorityRequest = testRequest.copy(priority = Priority.HIGH)
            val expectedResult = PipelineResult.success(highPriorityRequest, "Success")
            whenever(delegateStep.canExecute(any())).thenReturn(true)
            whenever(delegateStep.execute(any())).thenReturn(expectedResult)

            // When
            val canExecute = wrapper.canExecute(highPriorityRequest)
            val result = wrapper.execute(highPriorityRequest)

            // Then
            assertTrue(canExecute)
            verify(delegateStep).execute(argThat { request ->
                request.metadata["stepConfiguration"] == configuration
            })
            assertEquals(expectedResult, result)
        }

        @Test
        fun `should not execute when condition fails but still provide correct properties`() {
            // Given
            whenever(delegateStep.stepName).thenReturn("test-step")
            whenever(delegateStep.description).thenReturn("Test Step Description")
            val wrapper = ConfigurableStepWrapper(delegateStep, emptyMap(), "priority=HIGH")
            val lowPriorityRequest = testRequest.copy(priority = Priority.LOW)

            // When & Then
            assertFalse(wrapper.canExecute(lowPriorityRequest))
            assertEquals("test-step", wrapper.stepName)
            assertEquals("Test Step Description", wrapper.description)
        }
    }
}