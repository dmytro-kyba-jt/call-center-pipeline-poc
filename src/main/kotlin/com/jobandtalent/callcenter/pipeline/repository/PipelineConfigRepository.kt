package com.jobandtalent.callcenter.pipeline.repository

import com.jobandtalent.callcenter.pipeline.domain.UseCasePipelineConfig
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineConfigRepository {
    private val configs = mutableMapOf<String, UseCasePipelineConfig>()

    fun findByUseCase(useCase: String): UseCasePipelineConfig? =
        configs.values.firstOrNull { it.useCase == useCase && it.isActive }

    fun save(config: UseCasePipelineConfig): UseCasePipelineConfig {
        configs[config.id] = config.copy(updatedAt = LocalDateTime.now())
        return configs[config.id]!!
    }

    fun findAll(): List<UseCasePipelineConfig> = configs.values.toList()

    fun deleteByUseCase(useCase: String): Boolean {
        val config = findByUseCase(useCase)
        return if (config != null) {
            configs.remove(config.id) != null
        } else false
    }
}