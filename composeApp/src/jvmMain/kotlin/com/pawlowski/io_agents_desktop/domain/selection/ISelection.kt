package com.pawlowski.io_agents_desktop.domain.selection

import kotlinx.coroutines.flow.Flow

interface ISelection {
    suspend fun requestGraphFlow(): Unit

    fun observeGraphFlow(): Flow<Unit>

    suspend fun handleGraphFlow(index: Unit)
}
