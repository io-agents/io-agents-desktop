package com.pawlowski.io_agents_desktop.domain.acceptance

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import com.pawlowski.io_agents_desktop.data.WorkflowNodeTracker

const val ACCEPTANCE = "ACCEPT"

val ACCEPTANCE_SYSTEM_PROMPT = "Please answer with either only 'ACCEPT' or instructions on what is missing to ACCEPT."

inline fun <reified Input> AIAgentSubgraphBuilderBase<*, *>.acceptanceNode(
    acceptanceUseCase: IAcceptance,
    workflowNodeTracker: WorkflowNodeTracker,
) =
    node<Input, AcceptanceResult<Input>>(name = "AcceptanceNode") { input ->
        workflowNodeTracker.trackNodeExecution("AcceptanceNode", "Acceptance")
        llm.writeSession {
            val userAcceptanceInput = acceptanceUseCase.requestUserAcceptance(llmResult = "$input\n$ACCEPTANCE_SYSTEM_PROMPT")
            val isAccepted = (userAcceptanceInput.uppercase() == ACCEPTANCE)

            AcceptanceResult<Input>(
                response = input,
                accepted = isAccepted,
                correctionsNeeded = userAcceptanceInput.takeIf { !isAccepted },
            )
        }
    }

