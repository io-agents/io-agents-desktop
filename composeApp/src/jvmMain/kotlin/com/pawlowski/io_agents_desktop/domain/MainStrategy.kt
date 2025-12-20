package com.pawlowski.io_agents_desktop.domain

import ai.koog.agents.core.dsl.builder.strategy
import com.pawlowski.io_agents_desktop.data.WorkflowNodeTracker
import com.pawlowski.io_agents_desktop.domain.acceptance.IAcceptance
import com.pawlowski.io_agents_desktop.domain.clarification.IClarificationUseCase
import com.pawlowski.io_agents_desktop.domain.scenarios.SADSubgraph
import com.pawlowski.io_agents_desktop.domain.useCase.UseCaseDiagramInput
import com.pawlowski.io_agents_desktop.domain.useCase.UseCaseDiagramOutput
import com.pawlowski.io_agents_desktop.domain.useCase.useCaseDiagramSubgraph

fun mainStrategy(
    clarificationUseCase: IClarificationUseCase,
    acceptance: IAcceptance,
    workflowNodeTracker: WorkflowNodeTracker,
) = strategy<UseCaseDiagramInput, UseCaseDiagramOutput>("MAS-workflow") {
    workflowNodeTracker.trackNodeExecution("start", "Start")
    
    val useCaseDiagramSubgraph by useCaseDiagramSubgraph(
        clarificationUseCase = clarificationUseCase,
        acceptance = acceptance,
        workflowNodeTracker = workflowNodeTracker,
    )
    val sadSubgraph by SADSubgraph()

    nodeStart then useCaseDiagramSubgraph then nodeFinish
    
    // Track finish when workflow completes
    // Note: Individual node tracking happens in each node implementation
}

