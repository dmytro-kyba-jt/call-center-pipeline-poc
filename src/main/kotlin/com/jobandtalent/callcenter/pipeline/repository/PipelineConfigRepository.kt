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
        val updatedConfig = config.copy(updatedAt = LocalDateTime.now())

        // Remove any existing config with the same useCase first
        val existingKey = configs.entries.firstOrNull { it.value.useCase == config.useCase }?.key
        if (existingKey != null) {
            configs.remove(existingKey)
            println("🔄 Removed existing config for useCase: ${config.useCase}")
        }

        configs[updatedConfig.id] = updatedConfig
        println("💾 Saved config: ${updatedConfig.useCase} with ${updatedConfig.steps.size} steps")
        return updatedConfig
    }

    fun findAll(): List<UseCasePipelineConfig> = configs.values.toList()

    fun deleteByUseCase(useCase: String): Boolean {
        val existingEntry = configs.entries.firstOrNull { it.value.useCase == useCase }
        return if (existingEntry != null) {
            configs.remove(existingEntry.key)
            println("🗑️ Deleted config for useCase: $useCase")
            true
        } else {
            println("⚠️ No config found to delete for useCase: $useCase")
            false
        }
    }

    fun findById(id: String): UseCasePipelineConfig? = configs[id]

    fun deleteById(id: String): Boolean {
        return configs.remove(id) != null
    }

    // Debug method to see all stored configs
    fun debugPrint() {
        println("📊 Current stored configurations:")
        configs.forEach { (id, config) ->
            println("  - ID: $id, UseCase: ${config.useCase}, Steps: ${config.steps.size}, Updated: ${config.updatedAt}")
        }
    }
}