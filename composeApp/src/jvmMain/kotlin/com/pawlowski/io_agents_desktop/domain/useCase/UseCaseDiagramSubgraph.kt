package com.pawlowski.io_agents_desktop.domain.useCase

import ai.koog.agents.core.agent.entity.ToolSelectionStrategy
import ai.koog.agents.core.dsl.builder.AIAgentGraphStrategyBuilder
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.prompt.dsl.prompt
import com.pawlowski.io_agents_desktop.data.WorkflowNodeTracker
import com.pawlowski.io_agents_desktop.domain.acceptance.IAcceptance
import com.pawlowski.io_agents_desktop.domain.acceptance.acceptanceNode
import com.pawlowski.io_agents_desktop.domain.clarification.IClarificationUseCase
import com.pawlowski.io_agents_desktop.domain.clarification.clarificableNode
import com.pawlowski.io_agents_desktop.domain.plantUml.diagramErrorCorrectorNode
import com.pawlowski.io_agents_desktop.domain.plantUml.generateUmlImage

fun AIAgentGraphStrategyBuilder<*, *>.useCaseDiagramSubgraph(
    clarificationUseCase: IClarificationUseCase,
    acceptance: IAcceptance,
    workflowNodeTracker: WorkflowNodeTracker,
): AIAgentSubgraphDelegate<UseCaseDiagramInput, UseCaseDiagramOutput> =
    subgraph<UseCaseDiagramInput, UseCaseDiagramOutput>(
        name = "UseCaseDiagramSubgraph",
        toolSelectionStrategy = ToolSelectionStrategy.NONE,
    ) {
        val setPromptNode by setUseCasePromptNode(workflowNodeTracker)
        val clarificationNode by clarificableNode<UseCaseDiagramInput>(clarificationUseCase, workflowNodeTracker)
        val generateDiagramNode by generateDiagramNode(workflowNodeTracker)
        val diagramCorrectorNode by diagramErrorCorrectorNode(workflowNodeTracker)
        val acceptanceNode by acceptanceNode<UseCaseDiagramOutput>(acceptanceUseCase = acceptance, workflowNodeTracker = workflowNodeTracker)

        edge(nodeStart forwardTo setPromptNode)
        edge(setPromptNode forwardTo clarificationNode)
        edge(clarificationNode forwardTo generateDiagramNode)
        edge(
            generateDiagramNode forwardTo diagramCorrectorNode onCondition { result ->
                result.isFailure
            },
        )
        edge(diagramCorrectorNode forwardTo generateDiagramNode)
        edge(
            generateDiagramNode forwardTo acceptanceNode onCondition { result ->
                result.isSuccess
            } transformed { input ->
                UseCaseDiagramOutput(plantUmlText = input.getOrThrow())
            },
        )
        edge(
            acceptanceNode forwardTo nodeFinish onCondition {
                it.accepted
            } transformed {
                it.response
            },
        )
        edge(
            acceptanceNode forwardTo setPromptNode onCondition {
                !it.accepted
            } transformed {
                UseCaseDiagramInput(
                    plainTextUseCaseDescription =
                        """
                            Corrections to be done: ${it.correctionsNeeded}
                            Current diagram to be improved: ${it.response.plantUmlText}
                        """.trimMargin(),
                )
            },
        )
    }

private fun AIAgentSubgraphBuilderBase<*, *>.setUseCasePromptNode(
    workflowNodeTracker: WorkflowNodeTracker,
) =
    node<UseCaseDiagramInput, UseCaseDiagramInput>("change_prompt") { input ->
        workflowNodeTracker.trackNodeExecution("change_prompt", "Change Prompt")
        llm.writeSession {
            rewritePrompt {
                prompt(id = "Use case diagram subgraph prompt") {
                    system(
                        """
                        You are the Use Case Diagram Modeler in a Multi-Agent System (MAS) for software modeling.
                        Your goal is to create accurate and comprehensive use case diagrams based on user requirements.
                        Diagams should be in PlantUML format and only Use Case Diagrams!
                        """.trimIndent(),
                    )

                    user("Use case description: $input")
                }
            }
            input
        }
    }

private fun AIAgentSubgraphBuilderBase<*, *>.generateDiagramNode(
    workflowNodeTracker: WorkflowNodeTracker,
) =
    node<String, Result<String>>("use_case_diagram_generator") { input ->
        workflowNodeTracker.trackNodeExecution("use_case_diagram_generator", "Generate Diagram")
        println("Generating diagram...")
        runCatching {
            generateUmlImage(
                umlSource = input,
                outputPath = "use_case_diagram.png",
            )
            println("Use case diagram generated and saved to use_case_diagram.png")
            input
        }
    }

