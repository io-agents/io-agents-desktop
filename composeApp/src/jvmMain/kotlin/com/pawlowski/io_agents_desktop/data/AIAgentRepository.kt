package com.pawlowski.io_agents_desktop.data

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.feature.handler.agent.AgentCompletedContext
import ai.koog.agents.core.feature.handler.agent.AgentStartingContext
import ai.koog.agents.core.feature.handler.llm.LLMCallCompletedContext
import ai.koog.agents.core.feature.handler.llm.LLMCallStartingContext
import ai.koog.agents.core.feature.handler.node.NodeExecutionStartingContext
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import com.pawlowski.io_agents_desktop.config.LLMConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AIAgentRepository(
    private val workflowNodeTracker: WorkflowNodeTracker,
) {
    private var apiKey: String? = null
    private var strategy: AIAgentGraphStrategy<String, String>? = null
    private var agent: AIAgent<String, String>? = null
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    fun initialize(
        apiKey: String,
        strategy: AIAgentGraphStrategy<String, String>,
    ) {
        this.apiKey = apiKey
        this.strategy = strategy
        createNewAgent()
    }

    private fun createNewAgent() {
        val currentStrategy = strategy ?: return

        when (LLMConfig.provider) {
            LLMConfig.Provider.OLLAMA -> {
                val promptExecutor = simpleOllamaAIExecutor(baseUrl = LLMConfig.OLLAMA_BASE_URL)
                val model =
                    LLModel(
                        provider = LLMProvider.Ollama,
                        id = LLMConfig.OLLAMA_MODEL_ID,
                        capabilities =
                            listOf(
                                LLMCapability.Temperature,
                                LLMCapability.Schema.JSON.Basic,
                                LLMCapability.Tools,
                            ),
                        contextLength = 40_960,
                    )
                agent = createAgent(promptExecutor, model, currentStrategy)
            }

            LLMConfig.Provider.GOOGLE -> {
                val currentApiKey =
                    apiKey?.takeIf { it.isNotBlank() }
                        ?: return
                val promptExecutor = simpleGoogleAIExecutor(currentApiKey)
                val model = LLMConfig.GOOGLE_MODEL
                agent = createAgent(promptExecutor, model, currentStrategy)
            }
        }
    }

    private fun createAgent(
        promptExecutor: SingleLLMPromptExecutor,
        model: LLModel,
        currentStrategy: AIAgentGraphStrategy<String, String>,
    ): AIAgent<String, String> {
        val agentConfig =
            AIAgentConfig(
                prompt = Prompt.build("mas-io-workflow") {},
                model = model,
                maxAgentIterations = 100,
            )
        return AIAgent(
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
                    onLLMCallStarting { context: LLMCallStartingContext ->
                        // Get the last executed node (the one that is making the LLM call)
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
                                    is Message.System -> {
                                        systemMessages.add(message.content)
                                    }

                                    is Message.User -> {
                                        userMessages.add(message.content)
                                    }

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

                            // Track LLM call with prompt but without response yet (will be updated in onLLMCallCompleted)
                            workflowNodeTracker.trackLLMCallStarting(
                                nodeId = nodeId,
                                systemMessages = systemMessages,
                                userMessages = userMessages,
                            )
                        }
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
                                    is Message.System -> {
                                        systemMessages.add(message.content)
                                    }

                                    is Message.User -> {
                                        userMessages.add(message.content)
                                    }

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

    suspend fun runAgent(input: String): Result<String> {
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
