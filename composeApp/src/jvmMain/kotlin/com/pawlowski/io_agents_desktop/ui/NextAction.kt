package com.pawlowski.io_agents_desktop.ui

/**
 * Represents available actions after completing a diagram generation.
 * Easy to extend with new options in the future.
 */
sealed class NextAction {
    abstract val displayText: String
    abstract val description: String

    object NewDiagram : NextAction() {
        override val displayText = "Nowy model"
        override val description = "Stwórz kolejny model"
    }

    object Exit : NextAction() {
        override val displayText = "Wyjście"
        override val description = "Zamknij aplikację"
    }

    object Continue : NextAction() {
        override val displayText = "Kontynuuj"
        override val description = "Przejdź do kolejnego etapu"
    }

    companion object {
        val allActions = listOf(NewDiagram, Exit, Continue)
        val endActions = listOf(NewDiagram, Exit)
    }
}
