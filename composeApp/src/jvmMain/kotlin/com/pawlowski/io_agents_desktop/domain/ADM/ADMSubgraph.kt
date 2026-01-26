package com.pawlowski.io_agents_desktop.domain.ADM

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

fun AIAgentGraphStrategyBuilder<*, *>.ADMDiagramSubgraph(
    clarification: IClarificationUseCase,
    acceptance: IAcceptance,
): AIAgentSubgraphDelegate<ADMInput, ADMOutput> =
    subgraph<ADMInput, ADMOutput>(
        name = "ADMSubgraph",
        toolSelectionStrategy = ToolSelectionStrategy.NONE,
    ) {
        val setPromptModellerSEQNode by setPromptModellerSEQNode()
        val setPromptModellerCONDNode by setPromptModellerCONDNode()
        val setPromptModellerALTNode by setPromptModellerALTNode()
        val setPromptModellerLOOPNode by setPromptModellerLOOPNode()
        val setPromptModellerPARANode by setPromptModellerPARANode()

        val clarificationNodeSEQ by clarificableNode<ADMInput>(clarification)
        val clarificationNodeCOND by clarificableNode<ADMInput>(clarification)
        val clarificationNodeALT by clarificableNode<ADMInput>(clarification)
        val clarificationNodeLOOP by clarificableNode<ADMInput>(clarification)
        val clarificationNodePARA by clarificableNode<ADMInput>(clarification)
        
        val acceptanceNode by acceptanceNode<ADMOutput>(acceptanceUseCase = acceptance)

        val mapClarificationAccept by node<String, ADMOutput> { input ->
            ADMOutput(
                activitiesDiagramText = input,
            )
        }

        val mapClarificationSEQ by node<String, ADMInput> { input ->
            ADMInput(
                scenariosActivitiesText = input,
            )
        }

        val mapClarificationCOND by node<String, ADMInput> { input ->
            ADMInput(
                scenariosActivitiesText = input,
            )
        }

        val mapClarificationALT by node<String, ADMInput> { input ->
            ADMInput(
                scenariosActivitiesText = input,
            )
        }

        val mapClarificationLOOP by node<String, ADMInput> { input ->
            ADMInput(
                scenariosActivitiesText = input,
            )
        }
        val criticNode by criticNode<ADMInput>(clarification)
        val mapCriticClarification by node<String, ADMInput> { input ->
            ADMInput(
                scenariosActivitiesText = input,
            )
        }


        edge(nodeStart forwardTo setPromptModellerSEQNode)
        edge(setPromptModellerSEQNode forwardTo criticNode)
        edge(criticNode forwardTo mapCriticClarification)

        edge(mapCriticClarification forwardTo clarificationNodeSEQ)
        edge(clarificationNodeSEQ forwardTo mapClarificationSEQ)
        edge(mapClarificationSEQ forwardTo setPromptModellerCONDNode)

        edge(setPromptModellerCONDNode forwardTo clarificationNodeCOND)
        edge(clarificationNodeCOND forwardTo mapClarificationCOND)
        edge(mapClarificationCOND forwardTo setPromptModellerALTNode)

        edge(setPromptModellerALTNode forwardTo clarificationNodeALT)
        edge(clarificationNodeALT forwardTo mapClarificationALT)
        edge(mapClarificationALT forwardTo setPromptModellerLOOPNode)

        edge(setPromptModellerLOOPNode forwardTo clarificationNodeLOOP)
        edge(clarificationNodeLOOP forwardTo mapClarificationLOOP)
        edge(mapClarificationLOOP forwardTo setPromptModellerPARANode)

        edge(setPromptModellerPARANode forwardTo clarificationNodePARA)
        edge(clarificationNodePARA forwardTo mapClarificationAccept)
        
        edge(mapClarificationAccept forwardTo acceptanceNode)

        edge(
            acceptanceNode forwardTo nodeFinish onCondition {
                it.accepted
            } transformed {
                it.response
            },
        )
        edge(
            acceptanceNode forwardTo setPromptModellerSEQNode onCondition {
                !it.accepted
            } transformed {
                ADMInput(
                    scenariosActivitiesText =
                        """
                            Corrections to be done: ${it.correctionsNeeded}
                            Current diagram to be improved: ${it.response.activitiesDiagramText}
                        """.trimMargin(),
                )
            },
        )
    }

private fun AIAgentSubgraphBuilderBase<*, *>.setPromptModellerSEQNode() =
    node<ADMInput, ADMInput>("change_prompt_seq") { input ->
        llm.writeSession {
            rewritePrompt {
                prompt(id = "ADM SEQ subgraph prompt") {
                    system(
                        """
                        You are the activity diagram modeller in a Multi-Agent System (MAS) for software modeling.
                        Your goal is to model activities based on provided scenarios and activities descriptions.
                        Keep in mind that <A> tags has already been indentified.
                        For all scenarioes create an model following provided guidelines:
                        identify and mark control flow structures using tag <SEQ>...</SEQ> for sequences of activities -
                        steps performed in a specific order, it may contain one or two activities.
                        Structural tags should enclose activities previously marked with <A> ... </A>. Nested structures 
                        are allowed and welcome — apply them logically. Do not tag actors — only activities!
                        Example: The user <SEQ><A>enters data</A>. The system <A>verifies the data</A> and <A> 
                        stores it in the database</A></SEQ>.
                        You may simplify previously used tags if needed.
                        """.trimIndent(),
                    )

                    user("Scenarios and activities - with identified A tags: : $input")
                }
            }
            input
        }
    }

private fun AIAgentSubgraphBuilderBase<*, *>.setPromptModellerCONDNode() =
    node<ADMInput, ADMInput>("change_prompt_cond") { input ->
        llm.writeSession {
            rewritePrompt {
                prompt(id = "ADM COND subgraph prompt") {
                    system(
                        """
                        You are the activity diagram modeller in a Multi-Agent System (MAS) for software modeling.
                        Your goal is to model activities based on provided scenarios and activities descriptions.
                        Keep in mind that <A> and <SEQ> tags has already been indentified.
                        For all scenarioes create an model following provided guidelines:
                        identify and mark control flow structures using tag <COND>...</COND> for conditional activities -
                        steps executed based on a condition.
                        Structural tags should enclose activities previously marked with <A> ... </A>. Nested structures 
                        are allowed and welcome — apply them logically. Do not tag actors — only activities!
                        Example : <COND> If <A>the data is incorrect</A>, the system <A>sends a notification</A> 
                        and <A>rejects the order</A></COND>.
                        You may simplify previously used tags if needed.
                        """.trimIndent(),
                    )

                    user("Scenarios and activities - with identified A, SEQ tags: $input")
                }
            }
            input
        }
    }

private fun AIAgentSubgraphBuilderBase<*, *>.setPromptModellerALTNode() =
    node<ADMInput, ADMInput>("change_prompt_alt") { input ->
        llm.writeSession {
            rewritePrompt {
                prompt(id = "ADM ALT subgraph prompt") {
                    system(
                        """
                        You are the activity diagram modeller in a Multi-Agent System (MAS) for software modeling.
                        Your goal is to model activities based on provided scenarios and activities descriptions.
                        Keep in mind that <A>, <SEQ> and <COND> tags has already been indentified.
                        For all scenarioes create an model following provided guidelines:
                        identify and mark control flow structures using tag <ALT>  ... </ALT> for conditional 
                        activities - without an "else" branch
                        Structural tags should enclose activities previously marked with <A> ... </A>. Nested structures 
                        are allowed and welcome — apply them logically. Do not tag actors — only activities!
                        Example : <ALT> If <A>the data is valid</A>, the system <A>completes the order</A></ALT>.
                        You may simplify previously used tags if needed.
                        """.trimIndent(),
                    )

                    user("Scenarios and activities - with identified A, SEQ, COND tags: $input")
                }
            }
            input
        }
    }

private fun AIAgentSubgraphBuilderBase<*, *>.setPromptModellerLOOPNode() =
    node<ADMInput, ADMInput>("change_prompt_loop") { input ->
        llm.writeSession {
            rewritePrompt {
                prompt(id = "ADM LOOP subgraph prompt") {
                    system(
                        """
                        You are the activity diagram modeller in a Multi-Agent System (MAS) for software modeling.
                        Your goal is to model activities based on provided scenarios and activities descriptions.
                        Keep in mind that <A>, <SEQ>, <COND> and <ALT> tags has already been indentified.
                        For all scenarioes create an model following provided guidelines:
                        identify and mark control flow structures using tag <LOOP>  ... </LOOP> for loops - repeated activities.
                        Structural tags should enclose activities previously marked with <A> ... </A>. Nested structures 
                        are allowed and welcome — apply them logically. Do not tag actors — only activities!
                        Example : If <A>there are more records</A>, <LOOP>the system <A>processes the next record</A></LOOP>
                        You may simplify previously used tags if needed.
                        """.trimIndent(),
                    )

                    user("Scenarios and activities - with identified A, SEQ, COND, ALT tags: $input")
                }
            }
            input
        }
    }

private fun AIAgentSubgraphBuilderBase<*, *>.setPromptModellerPARANode() =
    node<ADMInput, ADMInput>("change_prompt_para") { input ->
        llm.writeSession {
            rewritePrompt {
                prompt(id = "ADM PARA subgraph prompt") {
                    system(
                        """
                        You are the activity diagram modeller in a Multi-Agent System (MAS) for software modeling.
                        Your goal is to model activities based on provided scenarios and activities descriptions.
                        Keep in mind that <A>, <SEQ>, <COND>, <ALT> and <LOOP> tags has already been indentified.
                        For all scenarioes create an model following provided guidelines:
                        identify and mark control flow structures using tag <PARA>  ... </PARA> for parallel activities - 
                        operations performed simultaneously.
                        Structural tags should enclose activities previously marked with <A> ... </A>. Nested structures 
                        are allowed and welcome — apply them logically. Do not tag actors — only activities!
                        Example : <PARA><A>the change history is updated</A> and <A>events are logged</A></PARA>.
                        You may simplify previously used tags if needed.
                        """.trimIndent(),
                    )

                    user("Scenarios and activities - with identified A, SEQ, COND, ALT, LOOP tags: $input")
                }
            }
            input
        }
    }