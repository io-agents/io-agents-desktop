package com.pawlowski.io_agents_desktop.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WorkflowNode(
    val id: String,
    val name: String,
    val timestamp: Long = System.currentTimeMillis(),
)

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
    
    fun reset() {
        _execution.value = WorkflowExecution()
    }
}

