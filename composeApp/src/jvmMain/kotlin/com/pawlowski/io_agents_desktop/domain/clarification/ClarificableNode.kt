package com.pawlowski.io_agents_desktop.domain.clarification

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import com.pawlowski.io_agents_desktop.data.WorkflowNodeTracker

const val CLARIFICATION_PREFIX = "<<CLARIFICATION>>"

val CLARIFICATION_SYSTEM_PROMPT =
    """
    Begin by thoroughly analyzing the task description provided by the user.
        If the task description is unclear, ask the user for clarification before planning (if so, 
        please start your response with $CLARIFICATION_PREFIX and then continue with your questions).
        Be sure you fully understand the requirements before proceeding.
    """.trimIndent()

inline fun <reified Input> AIAgentSubgraphBuilderBase<*, *>.clarificableNode(
    clarificationUseCase: IClarificationUseCase,
    workflowNodeTracker: WorkflowNodeTracker,
) =
    node<Input, String>(name = "ClarificableNode") {
        workflowNodeTracker.trackNodeExecution("ClarificableNode", "Clarification")
        llm.writeSession {
            updatePrompt {
                system(CLARIFICATION_SYSTEM_PROMPT)
            }

            var response: String
            while (true) {
                response = requestLLMWithoutTools().content

                if (CLARIFICATION_PREFIX in response.uppercase()) {
                    val clarification = clarificationUseCase.requestUserClarification(llmQuestions = response)

                    updatePrompt {
                        user("Clarification: $clarification")
                    }
                } else {
                    break
                }
            }
            response
        }
    }

