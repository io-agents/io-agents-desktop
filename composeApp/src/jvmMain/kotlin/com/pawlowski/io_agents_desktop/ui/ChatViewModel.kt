package com.pawlowski.io_agents_desktop.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pawlowski.io_agents_desktop.domain.ChatUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val diagramImagePath: String? = null,
)

data class ChatState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            text = "Cześć! 👋 Jestem Twoim asystentem do generacji modeli opisu oprogramowania. \n\nMogę pomóc Ci:\n• Stworzyć diagram przypadków użycia na podstawie opisu\n• Wyjaśnić niejasności w wymaganiach\n• Wygenerować kod PlantUML\n• Wyciągnąć scenariusze i aktywności\n• Zamodelować diagram aktywności \n\n Co chciałbyś zrobić? Opisz swój projekt lub wymagania, a ja pomogę Ci zamodelować co zechcesz!",
            isUser = false,
        ),
    ),
    val isLoading: Boolean = false,
    val currentClarificationRequest: String? = null,
    val currentAcceptanceRequest: String? = null,
    val inputText: String = "",
    val isCompleted: Boolean = true, // blocks writing during selection
    val availableNextActions: List<NextAction> = emptyList(),
    val diagramLevel: Int = 0,
)

class ChatViewModel(
    private val chatUseCase: ChatUseCase,
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()
    
    // Store the last generated diagram path to show it in acceptance request
    private var lastDiagramPath: String? = null
    
    // Workflow execution state
    val workflowExecution = chatUseCase.workflowExecution

    // API key
    private val apiKey = System.getenv("GOOGLE_API_KEY") ?: ""
    
    private val maxDiagramLevel = 3

    init {
        // Initialize with API key from environment
        if (apiKey.isNotEmpty()) {
            chatUseCase.initialize(apiKey)
        }

        // Observe clarification requests
        chatUseCase.observeClarificationRequests()
            .onEach { request ->
                _state.update { currentState ->
                    currentState.copy(
                        currentClarificationRequest = request,
                        isLoading = false, // Stop loading when waiting for user clarification
                        messages = currentState.messages + ChatMessage(
                            text = "🤔 Zanim przejdę dalej, chciałbym lepiej zrozumieć Twoje wymagania:\n\n$request\n\nProszę, odpowiedz na te pytania, żebym mógł stworzyć dokładniejszy model.",
                            isUser = false,
                        ),
                    )
                }
            }
            .launchIn(viewModelScope)

        // Observe acceptance requests
        chatUseCase.observeAcceptanceRequests()
            .onEach { request ->
                // Diagram should already be saved at this point (generated in generateDiagramNode)
                // Use the standard path where diagram is saved
                val diagramPath = if (_state.value.diagramLevel == 1) "use_case_diagram.png" else null // diffrent behaviour only for UCD
                val isDiagram = if (_state.value.diagramLevel == 1) "diagram" else "model"
                
                _state.update { currentState ->
                        currentState.copy(
                        currentAcceptanceRequest = request,
                        isLoading = false, // Stop loading when waiting for user acceptance
                        messages = currentState.messages + ChatMessage(
                            text = "✅ Stworzyłem $isDiagram! Sprawdź proszę powyżej.\n\nJeśli wszystko wygląda dobrze, napisz 'ACCEPT'. Jeśli chcesz coś zmienić, opisz co dokładnie. \n$request",
                            isUser = false,
                            diagramImagePath = diagramPath, // Show the diagram in acceptance request
                        ),
                    )
                }
                lastDiagramPath = diagramPath
            }
            .launchIn(viewModelScope)

        // Observe processing state
        chatUseCase.isProcessing
            .onEach { isLoading ->
                _state.update { it.copy(isLoading = isLoading) }
            }
            .launchIn(viewModelScope)
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _state.value.isLoading) return

        val userMessage = ChatMessage(text = text, isUser = true)
        _state.update { currentState ->
            currentState.copy(
                messages = currentState.messages + userMessage,
                inputText = "",
            )
        }

        viewModelScope.launch {
            val result = chatUseCase.processMessage(text)
            result.fold(
                onSuccess = { output ->
                    val diagramPath = lastDiagramPath
                    _state.update { currentState ->
                        currentState.copy(
                            messages = currentState.messages + ChatMessage(
                                text = "🎉 Model został wygenerowany pomyślnie!",
                                isUser = false,
                                diagramImagePath = diagramPath,
                            ),
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { currentState ->
                        currentState.copy(
                            messages = currentState.messages + ChatMessage(
                                text = "Wystąpił błąd: ${error.message}",
                                isUser = false,
                            ),
                        )
                    }
                },
            )
        }
    }

    fun handleClarificationResponse(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(text = text, isUser = true)
        _state.update { currentState ->
            currentState.copy(
                messages = currentState.messages + userMessage,
                inputText = "",
                currentClarificationRequest = null,
                isLoading = true, // Resume loading after sending clarification response
            )
        }

        viewModelScope.launch {
            chatUseCase.handleClarification(text)
        }
    }

    fun handleAcceptanceResponse(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(text = text, isUser = true)
        val isAccepted = text.trim().uppercase() == "ACCEPT"
        
        _state.update { currentState ->
            currentState.copy(
                messages = currentState.messages + userMessage,
                inputText = "",
                currentAcceptanceRequest = null,
                isLoading = !isAccepted, // Only resume loading if not accepted (will need corrections)
            )
        }

        viewModelScope.launch {
            chatUseCase.handleAcceptance(text)
            
            // If accepted, show completion menu
            if (isAccepted) {
                showCompletionMenu()
            }
        }
    }
    
    private fun showCompletionMenu() {
        val actions = if (state.value.diagramLevel < maxDiagramLevel) NextAction.allActions else NextAction.endActions

        _state.update { currentState ->
            val menuText = buildString {
                appendLine("✅ Model został zaakceptowany!")
                appendLine()
                appendLine("Co chciałbyś zrobić dalej?")
                appendLine()
                actions.forEachIndexed { index, action ->
                    appendLine("${index + 1}. ${action.displayText} - ${action.description}")
                }
            }
            
            currentState.copy(
                messages = currentState.messages + ChatMessage(
                    text = menuText,
                    isUser = false,
                ),
                isCompleted = true,
                availableNextActions = actions,
            )
        }
    }

    private fun showSelectionMenu() {
        _state.update { currentState ->
            val menuText = buildString {
                appendLine("Witaj ponownie! Co chciałbyś zrobić?")
                appendLine()
                StartGraph.allActions.forEachIndexed { index, action ->
                    appendLine("${index + 1}. ${action.displayText} - ${action.description}")
                }
            }
            
            currentState.copy(
                messages = currentState.messages + ChatMessage(
                    text = menuText,
                    isUser = false,
                ),
                isCompleted = false,
                availableNextActions = emptyList(),
            )
        }
    }

    fun handleStartGraphSelection(action: StartGraph) {
        when (action) {
            is StartGraph.UCD -> {
                // Reset agent and start UCD workflow
                chatUseCase.initialize(apiKey, 1)
                chatUseCase.resetAgent()
                _state.update { currentState ->
                    currentState.copy(
                        isCompleted = false,
                        availableNextActions = emptyList(),
                        messages = currentState.messages + ChatMessage(
                            text = "Świetnie! Stwórzmy diagram przypadków użycia. Opisz proszę, jaki diagram chciałbyś wygenerować.",
                            isUser = false,
                        ),
                        diagramLevel = 1,
                    )
                }
            }
            is StartGraph.SAD -> {
                // Reset agent and start SAD workflow
                chatUseCase.initialize(apiKey, 2)
                chatUseCase.resetAgent()
                _state.update { currentState ->
                    currentState.copy(
                        isCompleted = false,
                        availableNextActions = emptyList(),
                        messages = currentState.messages + ChatMessage(
                            text = "Świetnie! Wyciągnijmy scenariusze i aktywności z diagramu przypadków użycia. Proszę, podaj opis diagramu przypadków użycia.",
                            isUser = false,
                        ),
                        diagramLevel = 2,
                    )
                }
            }
            is StartGraph.ADM -> {
                // Reset agent and start ADM workflow
                chatUseCase.initialize(apiKey, 3)
                chatUseCase.resetAgent()
                _state.update { currentState ->
                    currentState.copy(
                        isCompleted = false,
                        availableNextActions = emptyList(),
                        messages = currentState.messages + ChatMessage(
                            text = "Świetnie! Stwórzmy diagram aktywności. Proszę, podaj opis scenariuszy i aktywności.",
                            isUser = false,
                        ),
                        diagramLevel = 3,
                    )
                }
            }
        }
    }
    
    fun handleNextAction(action: NextAction) {
        when (action) {
            is NextAction.NewDiagram -> {
                // Reset agent and start new diagram
                chatUseCase.resetAgent()
                _state.update { currentState ->
                    currentState.copy(
                        isCompleted = true, // blocks writing during selection
                        availableNextActions = emptyList(),
                        messages = currentState.messages + ChatMessage(
                            text = "Świetnie! Stwórzmy nowy model.",
                            isUser = false,
                        ),
                        diagramLevel = 0,
                    )
                }
            }
            is NextAction.Exit -> {
                // Exit application - this will be handled in UI
                _state.update { currentState ->
                    currentState.copy(
                        messages = currentState.messages + ChatMessage(
                            text = "Dziękuję za korzystanie z aplikacji! Do widzenia! 👋",
                            isUser = false,
                        ),
                    )
                }
            }
            is NextAction.Continue -> {
                _state.update { currentState ->
                    currentState.copy(
                        isCompleted = false,
                        availableNextActions = emptyList(),
                        isLoading = true,
                        messages = currentState.messages + ChatMessage(
                            text = "Świetnie! Kontynuujmy do następnego etapu.",
                            isUser = false,
                        ),
                        diagramLevel = currentState.diagramLevel + 1,
                    )
                }
            }
        }
    }

    fun updateInputText(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    fun onSendClick() {
        val currentState = _state.value
        when {
            currentState.currentClarificationRequest != null -> {
                handleClarificationResponse(currentState.inputText)
            }
            currentState.currentAcceptanceRequest != null -> {
                handleAcceptanceResponse(currentState.inputText)
            }
            else -> {
                sendMessage(currentState.inputText)
            }
        }
    }

    fun dispose() {
        viewModelScope.cancel()
    }
}

