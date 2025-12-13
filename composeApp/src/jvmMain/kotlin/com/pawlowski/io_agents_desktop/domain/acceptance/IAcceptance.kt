package com.pawlowski.io_agents_desktop.domain.acceptance

import kotlinx.coroutines.flow.Flow

interface IAcceptance {
    suspend fun requestUserAcceptance(llmResult: String): String

    fun observeAcceptenceRequests(): Flow<String>

    suspend fun handleAcceptence(acceptance: String)
}

