package com.jobandtalent.callcenter.pipeline.pipeline.impl

import AcceptanceRulesStep
import com.jobandtalent.callcenter.pipeline.domain.CommunicationRequest
import com.jobandtalent.callcenter.pipeline.domain.CommunicationType
import com.jobandtalent.callcenter.pipeline.domain.RequestStatus
import com.jobandtalent.callcenter.pipeline.pipeline.CommunicationPipelineExecutor
import com.jobandtalent.callcenter.pipeline.pipeline.PipelineStepRegistry
import com.jobandtalent.callcenter.pipeline.pipeline.steps.CommunicationProviderStep
import com.jobandtalent.callcenter.pipeline.pipeline.steps.DataStorageStep
import com.jobandtalent.callcenter.pipeline.pipeline.steps.ExclusionRulesStep
import com.jobandtalent.callcenter.pipeline.pipeline.steps.SchedulerStep
import com.jobandtalent.callcenter.pipeline.repository.CommunicationRequestRepository
import com.jobandtalent.callcenter.pipeline.service.CommunicationProviderService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommunicationPipelineTest {

    private lateinit var stepRegistry: PipelineStepRegistry
    private lateinit var pipelineExecutor: CommunicationPipelineExecutor
    private lateinit var mockRepository: CommunicationRequestRepository
    private lateinit var mockProviderService: CommunicationProviderService

    @BeforeEach
    fun setup() {
        stepRegistry = PipelineStepRegistryImpl()
        mockRepository = mock()
        mockProviderService = mock()

        // Register pipeline steps
        stepRegistry.registerStep(AcceptanceRulesStep())
        stepRegistry.registerStep(DataStorageStep(mockRepository))
        stepRegistry.registerStep(SchedulerStep())
        stepRegistry.registerStep(ExclusionRulesStep())
        stepRegistry.registerStep(CommunicationProviderStep(mockProviderService))

        pipelineExecutor = CommunicationPipelineExecutorImpl(stepRegistry)
    }

    @Test
    fun `should execute full pipeline successfully`() = runTest {
        // Given
        val request = CommunicationRequest(
            workerId = "worker-123",
            communicationType = CommunicationType.CALL,
            payload = mapOf("message" to "Test call"),
            useCase = "urgent-notification"
        )

        whenever(mockRepository.save(any())).thenReturn(request)
        whenever(mockProviderService.sendCommunication(any())).thenReturn(
            mapOf("callId" to "call-123", "status" to "sent")
        )

        // When
        val result = pipelineExecutor.execute(request)

        // Then
        assertTrue(result.success)
        assertEquals(RequestStatus.COMPLETED, result.request.status)
        verify(mockRepository).save(any())
        verify(mockProviderService).sendCommunication(any())
    }

    @Test
    fun `should fail at acceptance rules for invalid request`() = runTest {
        // Given
        val invalidRequest = CommunicationRequest(
            workerId = "", // Invalid - empty worker ID
            communicationType = CommunicationType.CALL,
            payload = emptyMap(), // Invalid - empty payload
            useCase = ""
        )

        // When
        val result = pipelineExecutor.execute(invalidRequest)

        // Then
        assertTrue(!result.success)
        // TODO: check if failed because of acceptance rules
        assertEquals(RequestStatus.FAILED, result.request.status)
        verify(mockRepository, never()).save(any())
        verify(mockProviderService, never()).sendCommunication(any())
    }

    @Test
    fun `should fail at exclusion rules for blocked worker`() = runTest {
        // Given
        val blockedRequest = CommunicationRequest(
            workerId = "blocked-worker-1", // This worker is in exclusion list
            communicationType = CommunicationType.CALL,
            payload = mapOf("message" to "Test call"),
            useCase = "test-case"
        )

        whenever(mockRepository.save(any())).thenReturn(blockedRequest)

        // When
        val result = pipelineExecutor.execute(blockedRequest)

        // Then
        assertTrue(!result.success)
        // TODO: check if failed because of exclusion rules
        assertEquals(RequestStatus.FAILED, result.request.status)
        assertTrue(result.message.contains("excluded"))
        verify(mockRepository).save(any()) // Should reach data storage step
        verify(mockProviderService, never()).sendCommunication(any()) // Should not reach provider
    }

    @Test
    fun `should execute from specific step`() = runTest {
        // Given
        val request = CommunicationRequest(
            workerId = "worker-123",
            communicationType = CommunicationType.SMS,
            payload = mapOf("message" to "Test SMS"),
            useCase = "reminder"
        )

        whenever(mockProviderService.sendCommunication(any())).thenReturn(
            mapOf("smsId" to "sms-123", "status" to "sent")
        )

        // When - start from exclusion rules step (skip earlier steps)
        val result = pipelineExecutor.executeFromStep(request, "exclusionRules")

        // Then
        assertTrue(result.success)
        verify(mockRepository, never()).save(any()) // Should skip data storage
        verify(mockProviderService).sendCommunication(any()) // Should reach provider
    }
}
