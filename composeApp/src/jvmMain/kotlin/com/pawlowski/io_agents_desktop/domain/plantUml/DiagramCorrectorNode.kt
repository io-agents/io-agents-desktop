package com.pawlowski.io_agents_desktop.domain.plantUml

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase

internal fun AIAgentSubgraphBuilderBase<*, *>.diagramErrorCorrectorNode() =
    node<Result<String>, String>("diagramCorrector") { result ->
        result.getOrElse {
            llm.writeSession {
                updatePrompt {
                    user(
                        content = "Diagram generation failed with error: ${result.exceptionOrNull()?.message}. Please correct the PlantUML diagram.",
                    )
                }
                requestLLMWithoutTools().content
            }
        }
    }
