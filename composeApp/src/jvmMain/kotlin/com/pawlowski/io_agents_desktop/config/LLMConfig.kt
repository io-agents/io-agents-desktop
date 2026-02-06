package com.pawlowski.io_agents_desktop.config

import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.llm.LLModel

/**
 * Konfiguracja modelu LLM i dostawcy. Zmień te wartości, aby użyć innego modelu lub API.
 *
 * Zobacz README.md w katalogu projektu, aby dowiedzieć się:
 * - jakie wymagania muszą być spełnione dla wybranego providera,
 * - jak ustawić token autoryzacyjny (zmienna środowiskowa).
 */
object LLMConfig {
    /**
     * Dostawca modelu: Ollama (lokalny) lub Google (API w chmurze).
     */
    enum class Provider {
        OLLAMA,
        GOOGLE,
    }

    /** Wybierz dostawcę: OLLAMA lub GOOGLE */
    val provider: Provider = Provider.GOOGLE

    // --- Ollama (gdy provider == OLLAMA) ---

    /** Adres URL lokalnej instancji Ollama (domyślnie http://localhost:11434) */
    const val OLLAMA_BASE_URL: String = "http://localhost:11434"

    /**
     * Nazwa modelu w Ollama, np. "llama3.1:8b", "llama3.2", "mistral", "codellama".
     * Lista: uruchom `ollama list` w terminalu.
     */
    const val OLLAMA_MODEL_ID: String = "llama3.1:8b"

    // --- Google (gdy provider == GOOGLE) ---

    /**
     * Model od Google AI Studio. Wybierz model z listy dostępnych w objekcie GoogleModels, np. Gemini2_0Flash,
     * Gemini2_0Flash001, Gemini2_0FlashLite, Gemini2_5Pro, Gemini2_5Flash, Gemini2_5FlashLite
     */
    val GOOGLE_MODEL: LLModel = GoogleModels.Gemini2_5Flash

    /**
     * Nazwa zmiennej środowiskowej z tokenem/kluczem API.
     * Dla Google: ustaw np. GOOGLE_API_KEY (klucz z Google AI Studio).
     * Dla Ollama: token nie jest wymagany — aplikacja nie używa tej zmiennej przy OLLAMA.
     */
    const val API_KEY_ENV_VAR: String = "GOOGLE_API_KEY"
}
