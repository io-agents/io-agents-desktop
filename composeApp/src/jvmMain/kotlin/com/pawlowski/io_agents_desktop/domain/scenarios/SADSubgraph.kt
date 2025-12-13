package com.pawlowski.io_agents_desktop.domain.scenarios

import ai.koog.agents.core.agent.entity.ToolSelectionStrategy
import ai.koog.agents.core.dsl.builder.AIAgentGraphStrategyBuilder
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.dsl.builder.forwardTo

fun AIAgentGraphStrategyBuilder<*, *>.SADSubgraph(): AIAgentSubgraphDelegate<SADInput, SADOutput> =
    subgraph<SADInput, SADOutput>(
        name = "SADSubgraph",
        toolSelectionStrategy = ToolSelectionStrategy.NONE,
    ) {
        edge(
            nodeStart forwardTo nodeFinish transformed {
                SADOutput(
                    scenariosText = "TODO",
                    activitiesText = "TODO",
                )
            },
        )
    }

