package com.pawlowski.io_agents_desktop.domain.clarification

import kotlinx.coroutines.flow.Flow

interface IClarificationUseCase {
    suspend fun requestUserClarification(llmQuestions: String): String

    fun observeClarificationRequests(): Flow<String>

    suspend fun handleUserClarification(clarification: String)
}

