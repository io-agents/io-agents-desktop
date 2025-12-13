package com.pawlowski.io_agents_desktop.data

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import com.pawlowski.io_agents_desktop.domain.useCase.UseCaseDiagramInput
import com.pawlowski.io_agents_desktop.domain.useCase.UseCaseDiagramOutput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AIAgentRepository {
    private var agent: AIAgent<UseCaseDiagramInput, UseCaseDiagramOutput>? = null
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    fun initialize(apiKey: String, strategy: AIAgentGraphStrategy<UseCaseDiagramInput, UseCaseDiagramOutput>) {
        val promptExecutor = simpleGoogleAIExecutor(apiKey)

        val agentConfig =
            AIAgentConfig(
                prompt = Prompt.build("mas-io-workflow") {},
                model = GoogleModels.Gemini2_5Flash,
                maxAgentIterations = 30,
            )

        agent = AIAgent(
            promptExecutor = promptExecutor,
            strategy = strategy,
            agentConfig = agentConfig,
        )
    }

    suspend fun runAgent(input: UseCaseDiagramInput): Result<UseCaseDiagramOutput> {
        val currentAgent = agent ?: return Result.failure(IllegalStateException("Agent not initialized"))
        
        return try {
            _isProcessing.value = true
            val result = currentAgent.run(input)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isProcessing.value = false
        }
    }
}

