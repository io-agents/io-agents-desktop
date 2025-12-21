package com.pawlowski.io_agents_desktop.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LLMCallInfo(
    val prompt: String,
    val response: String,
    val timestamp: Long = System.currentTimeMillis(),
)

data class WorkflowNode(
    val id: String,
    val name: String,
    val timestamp: Long = System.currentTimeMillis(),
    val llmCalls: List<LLMCallInfo> = emptyList(),
) {
    val hasLLMCalls: Boolean get() = llmCalls.isNotEmpty()
    
    fun addLLMCall(call: LLMCallInfo): WorkflowNode {
        return copy(llmCalls = llmCalls + call)
    }
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
    fun addNode(node: WorkflowNode): WorkflowExecution {
        return copy(nodes = nodes + node)
    }
    
    fun addEdge(edge: WorkflowEdge): WorkflowExecution {
        return copy(edges = edges + edge)
    }
    
    fun getLastNodeId(): String? = nodes.lastOrNull()?.id
}

class WorkflowNodeTracker {
    private val _execution = MutableStateFlow(WorkflowExecution())
    val execution: StateFlow<WorkflowExecution> = _execution.asStateFlow()
    
    fun trackNodeExecution(nodeId: String, nodeName: String) {
        val node = WorkflowNode(id = nodeId, name = nodeName)
        val lastNodeId = _execution.value.getLastNodeId()
        
        val current = _execution.value
        var updated = current.addNode(node)
        if (lastNodeId != null) {
            updated = updated.addEdge(WorkflowEdge(fromNodeId = lastNodeId, toNodeId = nodeId))
        }
        _execution.value = updated
    }
    
    fun trackLLMCall(nodeId: String, prompt: String, response: String) {
        val current = _execution.value
        // Find the last occurrence of the node (most recently added) to handle repeated nodes
        val nodeIndex = current.nodes.indexOfLast { it.id == nodeId }
        if (nodeIndex >= 0) {
            val node = current.nodes[nodeIndex]
            val updatedNode = node.addLLMCall(LLMCallInfo(prompt = prompt, response = response))
            val updatedNodes = current.nodes.toMutableList()
            updatedNodes[nodeIndex] = updatedNode
            _execution.value = current.copy(nodes = updatedNodes)
        }
    }
    
    fun reset() {
        _execution.value = WorkflowExecution()
    }
}

