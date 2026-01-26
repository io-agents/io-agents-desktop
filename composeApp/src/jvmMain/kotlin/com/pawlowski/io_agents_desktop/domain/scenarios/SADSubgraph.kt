package com.pawlowski.io_agents_desktop.domain.SAD

import ai.koog.agents.core.agent.entity.ToolSelectionStrategy
import ai.koog.agents.core.dsl.builder.AIAgentGraphStrategyBuilder
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.prompt.dsl.prompt
import com.pawlowski.io_agents_desktop.domain.acceptance.IAcceptance
import com.pawlowski.io_agents_desktop.domain.acceptance.acceptanceNode
import com.pawlowski.io_agents_desktop.domain.clarification.IClarificationUseCase
import com.pawlowski.io_agents_desktop.domain.clarification.clarificableNode
import com.pawlowski.io_agents_desktop.domain.critics.criticNode

fun AIAgentGraphStrategyBuilder<*, *>.SADDiagramSubgraph(
    clarification: IClarificationUseCase,
    acceptance: IAcceptance,
): AIAgentSubgraphDelegate<SADInput, SADOutput> =
    subgraph<SADInput, SADOutput>(
        name = "SADSubgraph",
        toolSelectionStrategy = ToolSelectionStrategy.NONE,
    ) {
        val setPromptScenarios by setPromptScenariosNode()
        val setPromptActiviies by setPromptScenariosActivities()
        val clarificationNodeScenarios by clarificableNode<SADInput>(clarification)
        val clarificationNodeActivities by clarificableNode<SADInput>(clarification)
        val acceptanceNode by acceptanceNode<SADOutput>(acceptanceUseCase = acceptance)
        val criticNode by criticNode<SADInput>(clarification)
        val mapCriticClarification by node<String, SADInput> { input ->
            SADInput(
                plainTextUseCaseDescription = input,
            )
        }

        val mapClarificationNodeScenarios by node<String, SADInput> { input ->
            SADInput(
                plainTextUseCaseDescription = input,
            )
        }
        val mapClarificationNodeActivities by node<String, SADOutput> { input ->
            SADOutput(
                scenariosActivitiesText = input,
            )
        }

        edge(nodeStart forwardTo setPromptScenarios)
        edge(setPromptScenarios forwardTo criticNode)
        edge(criticNode forwardTo mapCriticClarification)
        edge(mapCriticClarification forwardTo clarificationNodeScenarios)
        edge(clarificationNodeScenarios forwardTo mapClarificationNodeScenarios)

        edge(mapClarificationNodeScenarios forwardTo setPromptActiviies)
        edge(setPromptActiviies forwardTo clarificationNodeActivities)
        edge(clarificationNodeActivities forwardTo mapClarificationNodeActivities)

        edge(mapClarificationNodeActivities forwardTo acceptanceNode)

        edge(
            acceptanceNode forwardTo nodeFinish onCondition {
                it.accepted
            } transformed {
                it.response
            },
        )
        edge(
            acceptanceNode forwardTo setPromptScenarios onCondition {
                !it.accepted
            } transformed {
                SADInput(
                    plainTextUseCaseDescription =
                        """
                            Corrections to be done: ${it.correctionsNeeded}
                            Current diagram to be improved: ${it.response.scenariosActivitiesText}
                        """.trimMargin(),
                )
            },
        )
    }

private fun AIAgentSubgraphBuilderBase<*, *>.setPromptScenariosNode() =
    node<SADInput, SADInput>("change_prompt") { input ->
        llm.writeSession {
            rewritePrompt {
                prompt(id = "SAD subgraph prompt") {
                    system(
                        """
                        You are the scenarios extractor in a Multi-Agent System (MAS) for software modeling.
                        Your goal is to extract those informatio from given user case diagram.
                        Write an list of scenarios with all activities relavant to each one of them.
                        """.trimIndent(),
                    )

                    user("Use case description: $input")
                }
            }
            input
        }
    }

private fun AIAgentSubgraphBuilderBase<*, *>.setPromptScenariosActivities() =
    node<SADInput, SADInput>("change_prompt") { input ->
        llm.writeSession {
            rewritePrompt {
                prompt(id = "SAD subgraph prompt") {
                    system(
                        """
                        You are the activity extractor in a Multi-Agent System (MAS) for software modeling.
                        Your goal is to extract those informatio from given list of scenarios.
                        For the given use case scenario in natural language, please apply the following tags:
                        - <A> ... </A> to denote primary activities, where the entire verb–noun phrase represents a single activity.
                        - <P> ... </P> to denote activity parameters (input data, attributes, or values necessary for performing the activity, if applicable).
                        Tag short, semantically related phrases rather than entire sentences. Each primary activity should include both the verb and its object as a single unit within <A> ... </A>. Tags should not encompass actors.
                        Ensure that each activity and its parameters are clearly and accurately tagged to reflect their roles within the scenario.
                        Example:
                        The user <A>selects a product</A> and provides the <P>product name</P> and <P>quantity</P>.
                        The system <A>initiates the payment process</A> for the <P>product name</P> and <P>quantity</P>.
                        """.trimIndent(),
                    )

                    user("Scenarios: $input")
                }
            }
            input
        }
    }