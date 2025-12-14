package com.pawlowski.io_agents_desktop.domain

import com.pawlowski.io_agents_desktop.data.AIAgentRepository
import com.pawlowski.io_agents_desktop.data.AcceptanceRepository
import com.pawlowski.io_agents_desktop.data.ClarificationRepository
import com.pawlowski.io_agents_desktop.domain.SAD.SADInput
import com.pawlowski.io_agents_desktop.domain.SAD.SADOutput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class ChatUseCase(
    private val agentRepository: AIAgentRepository,
    private val clarificationRepository: ClarificationRepository,
    private val acceptanceRepository: AcceptanceRepository,
) {
    fun initialize(apiKey: String) {
        val strategy = mainStrategy(
            clarification = clarificationRepository,
            acceptance = acceptanceRepository,
        )
        agentRepository.initialize(apiKey, strategy)
    }

    suspend fun processMessage(userMessage: String): Result<SADOutput> {
        val input = SADInput(plainTextUseCaseDescription = userMessage)
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
}

