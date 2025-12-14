package com.pawlowski.io_agents_desktop.domain

import ai.koog.agents.core.dsl.builder.strategy
import com.pawlowski.io_agents_desktop.domain.acceptance.IAcceptance
import com.pawlowski.io_agents_desktop.domain.clarification.IClarification
import com.pawlowski.io_agents_desktop.domain.SAD.SADInput
import com.pawlowski.io_agents_desktop.domain.SAD.SADOutput
import com.pawlowski.io_agents_desktop.domain.SAD.SADDiagramSubgraph

fun mainStrategy(
    clarification: IClarification,
    acceptance: IAcceptance,
) = strategy<SADInput, SADOutput>("MAS-workflow") {
    val SADDiagramSubgraph by SADDiagramSubgraph(
        clarification = clarification,
        acceptance = acceptance,
    )

    nodeStart then SADDiagramSubgraph then nodeFinish
}

