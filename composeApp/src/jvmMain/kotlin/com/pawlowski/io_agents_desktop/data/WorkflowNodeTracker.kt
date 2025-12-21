package com.pawlowski.io_agents_desktop.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LLMCallInfo(
    val systemMessages: List<String> = emptyList(),
    val userMessages: List<String> = emptyList(),
    val response: String = "",
    val timestamp: Long = System.currentTimeMillis(),
) {
    val isCompleted: Boolean get() = response.isNotEmpty()
    
    fun withResponse(newResponse: String): LLMCallInfo {
        return copy(response = newResponse)
    }
}

data class WorkflowNode(
    val id: String,
    val name: String,
    val timestamp: Long = System.currentTimeMillis(),
    val llmCalls: List<LLMCallInfo> = emptyList(),
) {
    val hasLLMCalls: Boolean get() = llmCalls.isNotEmpty()

    fun addLLMCall(call: LLMCallInfo): WorkflowNode = copy(llmCalls = llmCalls + call)
}

data class WorkflowEdge(
    val fromNodeId: String,
    val toNodeId: String,
    val timestamp: Long = System.currentTimeMillis(),
)

data class WorkflowExecution(
    val nodes: List<WorkflowNode> = emptyList(),
    val edges: List<WorkflowEdge> = emptyList(),
) {
    fun addNode(node: WorkflowNode): WorkflowExecution = copy(nodes = nodes + node)

    fun addEdge(edge: WorkflowEdge): WorkflowExecution = copy(edges = edges + edge)

    fun getLastNodeId(): String? = nodes.lastOrNull()?.id
}

class WorkflowNodeTracker {
    private val _execution = MutableStateFlow(WorkflowExecution())
    val execution: StateFlow<WorkflowExecution> = _execution.asStateFlow()

    fun trackNodeExecution(
        nodeId: String,
        nodeName: String,
    ) {
        val node = WorkflowNode(id = nodeId, name = nodeName)
        val lastNodeId = _execution.value.getLastNodeId()

        val current = _execution.value
        var updated = current.addNode(node)
        if (lastNodeId != null) {
            updated = updated.addEdge(WorkflowEdge(fromNodeId = lastNodeId, toNodeId = nodeId))
        }
        _execution.value = updated
    }

    fun trackLLMCallStarting(
        nodeId: String,
        systemMessages: List<String>,
        userMessages: List<String>,
    ) {
        val current = _execution.value
        // Find the last occurrence of the node (most recently added) to handle repeated nodes
        val nodeIndex = current.nodes.indexOfLast { it.id == nodeId }
        if (nodeIndex >= 0) {
            val node = current.nodes[nodeIndex]
            // Check if there's already an incomplete call (without response)
            val incompleteCallIndex = node.llmCalls.indexOfLast { !it.isCompleted }
            if (incompleteCallIndex >= 0) {
                // Update existing incomplete call with new prompt
                val existingCall = node.llmCalls[incompleteCallIndex]
                val updatedCall = existingCall.copy(
                    systemMessages = systemMessages,
                    userMessages = userMessages,
                )
                val updatedCalls = node.llmCalls.toMutableList()
                updatedCalls[incompleteCallIndex] = updatedCall
                val updatedNode = node.copy(llmCalls = updatedCalls)
                val updatedNodes = current.nodes.toMutableList()
                updatedNodes[nodeIndex] = updatedNode
                _execution.value = current.copy(nodes = updatedNodes)
            } else {
                // Create new incomplete call
                val updatedNode =
                    node.addLLMCall(
                        LLMCallInfo(
                            systemMessages = systemMessages,
                            userMessages = userMessages,
                            response = "", // Empty response - will be filled in trackLLMCall
                        ),
                    )
                val updatedNodes = current.nodes.toMutableList()
                updatedNodes[nodeIndex] = updatedNode
                _execution.value = current.copy(nodes = updatedNodes)
            }
        }
    }

    fun trackLLMCall(
        nodeId: String,
        systemMessages: List<String>,
        userMessages: List<String>,
        response: String,
    ) {
        val current = _execution.value
        // Find the last occurrence of the node (most recently added) to handle repeated nodes
        val nodeIndex = current.nodes.indexOfLast { it.id == nodeId }
        if (nodeIndex >= 0) {
            val node = current.nodes[nodeIndex]
            // Find the last incomplete call (without response) and update it
            val incompleteCallIndex = node.llmCalls.indexOfLast { !it.isCompleted }
            if (incompleteCallIndex >= 0) {
                // Update existing incomplete call with response
                val existingCall = node.llmCalls[incompleteCallIndex]
                val updatedCall = existingCall.withResponse(response)
                val updatedCalls = node.llmCalls.toMutableList()
                updatedCalls[incompleteCallIndex] = updatedCall
                val updatedNode = node.copy(llmCalls = updatedCalls)
                val updatedNodes = current.nodes.toMutableList()
                updatedNodes[nodeIndex] = updatedNode
                _execution.value = current.copy(nodes = updatedNodes)
            } else {
                // No incomplete call found - create new one (shouldn't happen if onLLMCallStarting was called)
                val updatedNode =
                    node.addLLMCall(
                        LLMCallInfo(
                            systemMessages = systemMessages,
                            userMessages = userMessages,
                            response = response,
                        ),
                    )
                val updatedNodes = current.nodes.toMutableList()
                updatedNodes[nodeIndex] = updatedNode
                _execution.value = current.copy(nodes = updatedNodes)
            }
        }
    }

    fun reset() {
        _execution.value = WorkflowExecution()
    }
}
