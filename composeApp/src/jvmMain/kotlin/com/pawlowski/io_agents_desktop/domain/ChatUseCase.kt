package com.pawlowski.io_agents_desktop.domain

import com.pawlowski.io_agents_desktop.data.AIAgentRepository
import com.pawlowski.io_agents_desktop.data.AcceptanceRepository
import com.pawlowski.io_agents_desktop.data.ClarificationRepository
import com.pawlowski.io_agents_desktop.data.WorkflowNodeTracker
import com.pawlowski.io_agents_desktop.domain.useCase.UseCaseDiagramInput
import com.pawlowski.io_agents_desktop.domain.useCase.UseCaseDiagramOutput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class ChatUseCase(
    private val agentRepository: AIAgentRepository,
    private val clarificationRepository: ClarificationRepository,
    private val acceptanceRepository: AcceptanceRepository,
    workflowNodeTracker: WorkflowNodeTracker,
) {
    fun initialize(apiKey: String, startState: Int = 1) {
        val strategy =
            mainStrategy(
                clarificationUseCase = clarificationRepository,
                acceptance = acceptanceRepository,
                startState = startState,
            )
        agentRepository.initialize(apiKey, strategy)
    }

    suspend fun processMessage(userMessage: String): Result<String> {
        val input = userMessage
        return agentRepository.runAgent(input)
    }

    val isProcessing: StateFlow<Boolean> = agentRepository.isProcessing

    fun observeClarificationRequests(): Flow<String> = clarificationRepository.observeClarificationRequests()

    fun observeAcceptanceRequests(): Flow<String> = acceptanceRepository.observeAcceptenceRequests()

    suspend fun handleClarification(clarification: String) {
        clarificationRepository.handleUserClarification(clarification)
    }

    suspend fun handleAcceptance(acceptance: String) {
        acceptanceRepository.handleAcceptence(acceptance)
    }

    fun resetAgent() {
        agentRepository.resetAgent()
    }

    val workflowExecution = workflowNodeTracker.execution
}
