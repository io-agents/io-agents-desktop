package com.pawlowski.io_agents_desktop.domain.selection

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase

inline fun <reified Input> AIAgentSubgraphBuilderBase<*, *>.graphFlowNode(
    selectionNode: ISelection
) =
    node<Input, Pair<Input, Unit>>(name = "GraphFlowNode") { input ->

        // Suspend the agent until the graph/UI selects a branch
        val selectedIndex = selectionNode.requestGraphFlow()

        // Resume the agent with the chosen edge
        input to selectedIndex
    }

