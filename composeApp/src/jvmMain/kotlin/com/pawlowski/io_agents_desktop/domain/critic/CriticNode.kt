package com.pawlowski.io_agents_desktop.domain.critics

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import com.pawlowski.io_agents_desktop.domain.clarification.IClarificationUseCase

val CLARIFICATION_SYSTEM_PROMPT =
    """
    Provide criticism of given input description. Find what is NOT clear, which points are mutually contradictionary,
    identify all sorts of problems in given input. Write criticism of that input to user.
    """.trimIndent()

inline fun <reified Input> AIAgentSubgraphBuilderBase<*, *>.criticNode(
    clarificationUseCase: IClarificationUseCase,
) =
    node<Input, String>(name = "CriticNodeInput") {
        llm.writeSession {
            updatePrompt {
                system(CLARIFICATION_SYSTEM_PROMPT)
            }

            var response: String = ""
            while (true) {
                response = requestLLMWithoutTools().content


                val clarification = clarificationUseCase.requestUserClarification(llmQuestions = response)

                updatePrompt {
                    user("Clarification: $clarification")
                }
  
            }
            response
        }
    }

