package com.pawlowski.io_agents_desktop.domain.plantUml

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import com.pawlowski.io_agents_desktop.data.WorkflowNodeTracker

internal fun AIAgentSubgraphBuilderBase<*, *>.diagramErrorCorrectorNode(
    workflowNodeTracker: WorkflowNodeTracker,
) =
    node<Result<String>, String>("diagramCorrector") { result ->
        workflowNodeTracker.trackNodeExecution("diagramCorrector", "Diagram Corrector")
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

