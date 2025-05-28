package com.jobandtalent.callcenter.pipeline.repository

import com.jobandtalent.callcenter.pipeline.domain.CommunicationRequest
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class CommunicationRequestRepository {

    private val storage = ConcurrentHashMap<String, CommunicationRequest>()

    fun save(request: CommunicationRequest): CommunicationRequest {
        storage[request.id] = request
        return request
    }

    fun findById(id: String): CommunicationRequest? = storage[id]

    fun findAll(): List<CommunicationRequest> = storage.values.toList()

    fun deleteById(id: String): Boolean = storage.remove(id) != null
}