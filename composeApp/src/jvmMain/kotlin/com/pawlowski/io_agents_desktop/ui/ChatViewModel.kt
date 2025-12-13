package com.pawlowski.io_agents_desktop.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pawlowski.io_agents_desktop.data.AcceptanceRepository
import com.pawlowski.io_agents_desktop.data.AIAgentRepository
import com.pawlowski.io_agents_desktop.data.ClarificationRepository
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
)

data class ChatState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            text = "Cześć! 👋 Jestem Twoim asystentem do tworzenia diagramów przypadków użycia.\n\nMogę pomóc Ci:\n• Stworzyć diagram przypadków użycia na podstawie opisu\n• Wyjaśnić niejasności w wymaganiach\n• Wygenerować kod PlantUML\n\nCo chciałbyś zrobić? Opisz swój projekt lub wymagania, a ja pomogę Ci stworzyć odpowiedni diagram!",
            isUser = false,
        ),
    ),
    val isLoading: Boolean = false,
    val currentClarificationRequest: String? = null,
    val currentAcceptanceRequest: String? = null,
    val inputText: String = "",
)

class ChatViewModel {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val agentRepository = AIAgentRepository()
    private val clarificationRepository = ClarificationRepository()
    private val acceptanceRepository = AcceptanceRepository()
    private val chatUseCase = ChatUseCase(
        agentRepository = agentRepository,
        clarificationRepository = clarificationRepository,
        acceptanceRepository = acceptanceRepository,
    )

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
                _state.update { currentState ->
                    currentState.copy(
                        currentAcceptanceRequest = request,
                        messages = currentState.messages + ChatMessage(
                            text = "✅ Stworzyłem diagram! Sprawdź proszę:\n\n$request\n\nJeśli wszystko wygląda dobrze, napisz 'ACCEPT'. Jeśli chcesz coś zmienić, opisz co dokładnie.",
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
                                text = "🎉 Diagram został wygenerowany pomyślnie!\n\nOto kod PlantUML:\n\n```\n${output.plantUmlText}\n```\n\nDiagram został również zapisany jako plik PNG: use_case_diagram.png",
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
            )
        }

        viewModelScope.launch {
            chatUseCase.handleClarification(text)
        }
    }

    fun handleAcceptanceResponse(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(text = text, isUser = true)
        _state.update { currentState ->
            currentState.copy(
                messages = currentState.messages + userMessage,
                inputText = "",
                currentAcceptanceRequest = null,
            )
        }

        viewModelScope.launch {
            chatUseCase.handleAcceptance(text)
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

