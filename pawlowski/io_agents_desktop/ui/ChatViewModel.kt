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
            text = "Cześć! 👋 Jestem Twoim asystentem do wyciągania scenariuszy i akywności. Proszę podaj mi swoją reprezentację UML!",
            isUser = false,
        ),
    ),
    val isLoading: Boolean = false,
    val currentClarificationRequest: String? = null,
    val currentAcceptanceRequest: String? = null,
    val inputText: String = "",
    val isCompleted: Boolean = false,
    val availableNextActions: List<NextAction> = emptyList(),
)

class ChatViewModel(
    private val chatUseCase: ChatUseCase,
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()
    

    init {
        // Initialize with API key from environment
        val apiKey = System.getenv("GOOGLE_API_KEY") ?: ""
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
                            text = "🤔 Zanim przejdę dalej, chciałbym lepiej zrozumieć Twoje wymagania:\n\n$request\n\nProszę, odpowiedz na te pytania, żebym mógł stworzyć dokładniejszy diagram.",
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
         
                _state.update { currentState ->
                    currentState.copy(
                        currentAcceptanceRequest = request,
                        isLoading = false, // Stop loading when waiting for user acceptance
                        messages = currentState.messages + ChatMessage(
                            text = "✅ Wyicągłem informację o scenariuszach i aktywnościach. Sprawdź proszę poniżej.\n\n$request\n\nJeśli wszystko wygląda dobrze, napisz 'ACCEPT'. Jeśli chcesz coś zmienić, opisz co dokładnie.",
                            isUser = false,
                        ),
                    )
                }
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
                    _state.update { currentState ->
                        currentState.copy(
                            messages = currentState.messages + ChatMessage(
                                text = "🎉 Scenariusze i aktywnośći zostały pomyślnie rozpoznane.",
                                isUser = false,
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
        _state.update { currentState ->
            val menuText = buildString {
                appendLine("✅ Wynik został zaakceptowany!")
                appendLine()
                appendLine("Co chciałbyś zrobić dalej?")
                appendLine()
                NextAction.allActions.forEachIndexed { index, action ->
                    appendLine("${index + 1}. ${action.displayText} - ${action.description}")
                }
            }
            
            currentState.copy(
                messages = currentState.messages + ChatMessage(
                    text = menuText,
                    isUser = false,
                ),
                isCompleted = true,
                availableNextActions = NextAction.allActions,
            )
        }
    }
    
    fun handleNextAction(action: NextAction) {
        when (action) {
            is NextAction.NewDiagram -> {
                // Reset agent and start new diagram
                chatUseCase.resetAgent()
                _state.update { currentState ->
                    currentState.copy(
                        isCompleted = false,
                        availableNextActions = emptyList(),
                        messages = currentState.messages + ChatMessage(
                            text = "Świetnie! Rozważmy kolejny diagram. Opisz proszę, z jakim diagramem przypadków użycia mamy do czynienia.",
                            isUser = false,
                        ),
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

