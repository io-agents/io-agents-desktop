package com.pawlowski.io_agents_desktop.data

import com.pawlowski.io_agents_desktop.domain.selection.ISelection
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

// Setup in case we need more complex subgraph transition logic in the future
class SelectionRepository : ISelection {
    private val graphFlowRequest = Channel<Unit>(Channel.RENDEZVOUS)
    private val graphFlowResponse = Channel<Unit>(Channel.RENDEZVOUS)

    override suspend fun requestGraphFlow() {
        graphFlowRequest.send(Unit)
        graphFlowResponse.receive()
    }

    override fun observeGraphFlow(): Flow<Unit> = graphFlowRequest.receiveAsFlow()

    override suspend fun handleGraphFlow(index: Unit) {
        graphFlowResponse.send(Unit)
    }
}