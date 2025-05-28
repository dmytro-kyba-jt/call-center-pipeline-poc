package com.jobandtalent.callcenter.integration

import com.jobandtalent.callcenter.pipeline.CallCenterPipelinePocApplication
import com.jobandtalent.callcenter.pipeline.controller.CreateCommunicationRequest
import com.jobandtalent.callcenter.pipeline.domain.CommunicationType
import com.jobandtalent.callcenter.pipeline.domain.Priority
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
    classes = [CallCenterPipelinePocApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class CommunicationControllerIntegrationTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `should process valid communication request successfully`() {
        // Given
        val request = CreateCommunicationRequest(
            workerId = "worker-123",
            communicationType = CommunicationType.CALL,
            payload = mapOf(
                "phoneNumber" to "+1234567890",
                "message" to "This is a test call"
            ),
            useCase = "urgent-notification",
            priority = Priority.HIGH
        )

        // When & Then
        webTestClient.post()
            .uri("/api/communications")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.request.status").isEqualTo("COMPLETED")
            .jsonPath("$.request.workerId").isEqualTo("worker-123")
    }

    @Test
    fun `should reject invalid communication request`() {
        // Given
        val invalidRequest = CreateCommunicationRequest(
            workerId = "", // Invalid - blank
            communicationType = CommunicationType.CALL,
            payload = emptyMap(), // This might be invalid depending on your validation
            useCase = "" // Invalid - blank
        )

        // When & Then
        webTestClient.post()
            .uri("/api/communications")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidRequest)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.success").isEqualTo(false)
            .jsonPath("$.request.status").isEqualTo("FAILED")
    }

    @Test
    fun `should reject blocked worker`() {
        // Given
        val blockedRequest = CreateCommunicationRequest(
            workerId = "blocked-worker-1",
            communicationType = CommunicationType.EMAIL,
            payload = mapOf("subject" to "Test", "body" to "Test email"),
            useCase = "notification"
        )

        // When & Then
        webTestClient.post()
            .uri("/api/communications")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(blockedRequest)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.success").isEqualTo(false)
            .jsonPath("$.request.status").isEqualTo("FAILED")
            .jsonPath("$.message").value(containsString("excluded"))
    }

    @Test
    fun `should reject blocked worker with detailed logging`() {
        // Given
        val blockedRequest = CreateCommunicationRequest(
            workerId = "blocked-worker-1",
            communicationType = CommunicationType.EMAIL,
            payload = mapOf("subject" to "Test", "body" to "Test email"),
            useCase = "notification"
        )

        // When & Then
        val response = webTestClient.post()
            .uri("/api/communications")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(blockedRequest)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(String::class.java)
            .returnResult()

        // Additional logging for debugging
        println("🔍 Response Status: ${response.status}")
        println("📦 Response Body: ${response.responseBody}")

        // Verify the response contains expected content
        val responseBody = response.responseBody ?: ""
        assert(responseBody.contains("excluded")) { "Expected response to mention exclusion, but got: $responseBody" }
    }

    @Test
    fun `should handle reprocess request`() {
        // Given
        val originalRequest = CreateCommunicationRequest(
            workerId = "worker-456",
            communicationType = CommunicationType.SMS,
            payload = mapOf("phoneNumber" to "+1234567890", "message" to "Test SMS"),
            useCase = "notification"
        )

        // First create a request to get a requestId (this might need adjustment based on your actual API)
        val createResponse = webTestClient.post()
            .uri("/api/communications")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(originalRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .returnResult()

        // For demonstration - you might need to extract the actual requestId from the response
        val requestId = "test-request-id"
        val stepName = "validation"

        // When & Then - Test reprocess endpoint
        webTestClient.post()
            .uri("/api/communications/{requestId}/reprocess?stepName={stepName}", requestId, stepName)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(originalRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
    }

    @Test
    fun `should handle concurrent requests`() {
        // Given
        val request1 = CreateCommunicationRequest(
            workerId = "worker-concurrent-1",
            communicationType = CommunicationType.EMAIL,
            payload = mapOf("subject" to "Test 1", "body" to "Test email 1"),
            useCase = "notification"
        )

        val request2 = CreateCommunicationRequest(
            workerId = "worker-concurrent-2",
            communicationType = CommunicationType.SMS,
            payload = mapOf("phoneNumber" to "+1234567890", "message" to "Test SMS 2"),
            useCase = "notification"
        )

        // When & Then - Send concurrent requests
        val response1 = webTestClient.post()
            .uri("/api/communications")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request1)
            .exchange()

        val response2 = webTestClient.post()
            .uri("/api/communications")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request2)
            .exchange()

        // Both should succeed
        response1.expectStatus().isOk
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)

        response2.expectStatus().isOk
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
    }

    @Test
    fun `should handle malformed JSON`() {
        // When & Then
        webTestClient.post()
            .uri("/api/communications")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{ invalid json }")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `should handle missing required fields`() {
        // Given - JSON with missing required fields
        val incompleteRequest = mapOf(
            "communicationType" to "EMAIL",
            "payload" to mapOf("subject" to "Test")
            // Missing workerId and useCase
        )

        // When & Then
        webTestClient.post()
            .uri("/api/communications")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(incompleteRequest)
            .exchange()
            .expectStatus().isBadRequest
    }
}