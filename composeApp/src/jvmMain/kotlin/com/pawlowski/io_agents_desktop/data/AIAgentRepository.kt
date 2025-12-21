package com.pawlowski.io_agents_desktop.data

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.feature.handler.agent.AgentCompletedContext
import ai.koog.agents.core.feature.handler.agent.AgentStartingContext
import ai.koog.agents.core.feature.handler.llm.LLMCallCompletedContext
import ai.koog.agents.core.feature.handler.node.NodeExecutionStartingContext
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.message.Message
import com.pawlowski.io_agents_desktop.domain.useCase.UseCaseDiagramInput
import com.pawlowski.io_agents_desktop.domain.useCase.UseCaseDiagramOutput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AIAgentRepository(
    private val workflowNodeTracker: WorkflowNodeTracker,
) {
    private var apiKey: String? = null
    private var strategy: AIAgentGraphStrategy<UseCaseDiagramInput, UseCaseDiagramOutput>? = null
    private var agent: AIAgent<UseCaseDiagramInput, UseCaseDiagramOutput>? = null
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    fun initialize(
        apiKey: String,
        strategy: AIAgentGraphStrategy<UseCaseDiagramInput, UseCaseDiagramOutput>,
    ) {
        this.apiKey = apiKey
        this.strategy = strategy
        createNewAgent()
    }

    private fun createNewAgent() {
        val currentApiKey = apiKey ?: return
        val currentStrategy = strategy ?: return

        val promptExecutor = simpleGoogleAIExecutor(currentApiKey)

        val agentConfig =
            AIAgentConfig(
                prompt = Prompt.build("mas-io-workflow") {},
                model = GoogleModels.Gemini2_5Flash,
                maxAgentIterations = 30,
            )

        agent =
            AIAgent(
                promptExecutor = promptExecutor,
                strategy = currentStrategy,
                agentConfig = agentConfig,
                installFeatures = {
                    install(EventHandler) {
                        onAgentStarting { _: AgentStartingContext<*> ->
                            workflowNodeTracker.trackNodeExecution("start", "Start")
                        }
                        onAgentCompleted { _: AgentCompletedContext ->
                            workflowNodeTracker.trackNodeExecution("finish", "Finish")
                        }
                        onNodeExecutionStarting { context: NodeExecutionStartingContext ->
                            val nodeId = context.node.id
                            val nodeName = context.node.name
                            workflowNodeTracker.trackNodeExecution(nodeId, nodeName)
                        }
                        onLLMCallCompleted { context: LLMCallCompletedContext ->
                            // Get the last executed node (the one that made the LLM call)
                            val nodeId =
                                workflowNodeTracker.execution.value.nodes
                                    .lastOrNull()
                                    ?.id

                            if (nodeId != null) {
                                // Extract prompt structure from context.prompt.messages
                                val systemMessages = mutableListOf<String>()
                                val userMessages = mutableListOf<String>()

                                context.prompt.messages.forEach { message ->
                                    when (message) {
                                        is Message.System -> systemMessages.add(message.content)
                                        is Message.User -> userMessages.add(message.content)
                                        is Message.Assistant -> {
                                            // Skip assistant messages in prompt
                                        }
                                        is Message.Tool.Call -> {
                                            // Skip tool calls in prompt
                                        }
                                        is Message.Tool.Result -> {
                                            // Skip tool results in prompt
                                        }
                                    }
                                }

                                // Get the first response or combine all responses
                                val response =
                                    context.responses.firstOrNull()?.content
                                        ?: context.responses.joinToString("\n\n") { it.content }

                                workflowNodeTracker.trackLLMCall(
                                    nodeId = nodeId,
                                    systemMessages = systemMessages,
                                    userMessages = userMessages,
                                    response = response,
                                )
                            }
                        }
                    }
                },
            )
    }

    fun resetAgent() {
        agent = null
        workflowNodeTracker.reset()
        createNewAgent()
    }

    suspend fun runAgent(input: UseCaseDiagramInput): Result<UseCaseDiagramOutput> {
        val currentAgent = agent ?: return Result.failure(IllegalStateException("Agent not initialized"))

        return try {
            _isProcessing.value = true
            val result = currentAgent.run(input)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isProcessing.value = false
        }
    }
}
