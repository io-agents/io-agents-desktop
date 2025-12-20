package com.pawlowski.io_agents_desktop.domain

import ai.koog.agents.core.dsl.builder.strategy
import com.pawlowski.io_agents_desktop.domain.acceptance.IAcceptance
import com.pawlowski.io_agents_desktop.domain.clarification.IClarificationUseCase
import com.pawlowski.io_agents_desktop.domain.scenarios.SADSubgraph
import com.pawlowski.io_agents_desktop.domain.useCase.UseCaseDiagramInput
import com.pawlowski.io_agents_desktop.domain.useCase.UseCaseDiagramOutput
import com.pawlowski.io_agents_desktop.domain.useCase.useCaseDiagramSubgraph

fun mainStrategy(
    clarificationUseCase: IClarificationUseCase,
    acceptance: IAcceptance,
) = strategy<UseCaseDiagramInput, UseCaseDiagramOutput>("MAS-workflow") {
    val useCaseDiagramSubgraph by useCaseDiagramSubgraph(
        clarificationUseCase = clarificationUseCase,
        acceptance = acceptance,
    )
    val sadSubgraph by SADSubgraph()

    nodeStart then useCaseDiagramSubgraph then nodeFinish
}
