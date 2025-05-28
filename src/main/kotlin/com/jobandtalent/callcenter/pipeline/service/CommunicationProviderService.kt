package com.jobandtalent.callcenter.pipeline.service

import com.jobandtalent.callcenter.pipeline.domain.CommunicationRequest
import com.jobandtalent.callcenter.pipeline.domain.CommunicationType
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CommunicationProviderService {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun sendCommunication(request: CommunicationRequest): Map<String, Any> {
        // Simulate async call to external provider
        delay(200)

        logger.info("Sending ${request.communicationType} to worker ${request.workerId}")

        // Simulate different provider responses
        return when (request.communicationType) {
            CommunicationType.CALL -> mapOf(
                "providerId" to "call-provider-1",
                "callId" to "call-${System.currentTimeMillis()}",
                "estimatedDuration" to "300 seconds"
            )

            CommunicationType.SMS -> mapOf(
                "providerId" to "sms-provider-1",
                "messageId" to "sms-${System.currentTimeMillis()}",
                "deliveryStatus" to "SENT"
            )

            CommunicationType.EMAIL -> mapOf(
                "providerId" to "email-provider-1",
                "emailId" to "email-${System.currentTimeMillis()}",
                "deliveryStatus" to "QUEUED"
            )

            CommunicationType.PUSH_NOTIFICATION -> mapOf(
                "providerId" to "push-provider-1",
                "notificationId" to "push-${System.currentTimeMillis()}",
                "deliveryStatus" to "DELIVERED"
            )
        }
    }

}
