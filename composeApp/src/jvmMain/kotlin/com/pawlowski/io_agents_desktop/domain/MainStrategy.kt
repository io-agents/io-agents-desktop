package com.pawlowski.io_agents_desktop.domain

import ai.koog.agents.core.dsl.builder.strategy
import com.pawlowski.io_agents_desktop.domain.acceptance.IAcceptance
import com.pawlowski.io_agents_desktop.domain.clarification.IClarificationUseCase
import com.pawlowski.io_agents_desktop.domain.useCase.UseCaseDiagramInput
import com.pawlowski.io_agents_desktop.domain.useCase.UseCaseDiagramOutput
import com.pawlowski.io_agents_desktop.domain.useCase.useCaseDiagramSubgraph
import com.pawlowski.io_agents_desktop.domain.SAD.SADDiagramSubgraph
import com.pawlowski.io_agents_desktop.domain.SAD.SADInput
import com.pawlowski.io_agents_desktop.domain.SAD.SADOutput
import com.pawlowski.io_agents_desktop.domain.ADM.ADMDiagramSubgraph
import com.pawlowski.io_agents_desktop.domain.ADM.ADMInput
import com.pawlowski.io_agents_desktop.domain.ADM.ADMOutput

fun mainStrategy(
    clarificationUseCase: IClarificationUseCase,
    acceptance: IAcceptance,
    startState: Int,
) = strategy<String, String>("MAS-workflow") {
    val useCaseDiagramSubgraph by useCaseDiagramSubgraph(
        clarificationUseCase = clarificationUseCase,
        acceptance = acceptance,
    )
    val sadSubgraph by SADDiagramSubgraph(
        clarification = clarificationUseCase,
        acceptance = acceptance,
    )
    val ADMSubgraph by ADMDiagramSubgraph(
        clarification = clarificationUseCase,
        acceptance = acceptance,
    )

    val UCDtransferSAD by node<UseCaseDiagramOutput, SADInput> { input ->
        SADInput(
            plainTextUseCaseDescription = input.plantUmlText,
        )
    }

    val SADtransferADM by node<SADOutput, ADMInput> { input ->
        ADMInput(
            scenariosActivitiesText = input.scenariosActivitiesText,
        )
    }

    val finishTransfer by node<ADMOutput, String> { input ->
        input.activitiesDiagramText
    }

    val startTransferADM by node<String, ADMInput> { input ->
        ADMInput(
            scenariosActivitiesText = input,
        )
    }

    val startTransferSAD by node<String, SADInput> { input ->
        SADInput(
            plainTextUseCaseDescription = input,
        )
    }

    val startTransferUCD by node<String, UseCaseDiagramInput> { input ->
        UseCaseDiagramInput(
            plainTextUseCaseDescription = input,
        )
    }

    if (startState == 1){
        nodeStart then startTransferUCD then useCaseDiagramSubgraph then UCDtransferSAD then 
        sadSubgraph then SADtransferADM then ADMSubgraph then finishTransfer then  nodeFinish
    } else if (startState == 2){
        nodeStart then startTransferSAD then sadSubgraph then SADtransferADM then 
        ADMSubgraph then finishTransfer then nodeFinish
    } else {
        nodeStart then startTransferADM then ADMSubgraph then finishTransfer then nodeFinish
    }
  }
