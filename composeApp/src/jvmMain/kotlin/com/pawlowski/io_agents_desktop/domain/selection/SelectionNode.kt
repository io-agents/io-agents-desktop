package com.pawlowski.io_agents_desktop.domain.selection

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase

inline fun <reified Input> AIAgentSubgraphBuilderBase<*, *>.graphFlowNode(
    selectionNode: ISelection
) =
    node<Input, Input>(name = "GraphFlowNode") { input ->
        selectionNode.requestGraphFlow()
        input
    }
