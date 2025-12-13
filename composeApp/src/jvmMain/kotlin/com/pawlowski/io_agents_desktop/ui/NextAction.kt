package com.pawlowski.io_agents_desktop.ui

/**
 * Represents available actions after completing a diagram generation.
 * Easy to extend with new options in the future.
 */
sealed class NextAction {
    abstract val displayText: String
    abstract val description: String

    object NewDiagram : NextAction() {
        override val displayText = "Nowy diagram"
        override val description = "Stwórz kolejny diagram przypadków użycia"
    }

    object Exit : NextAction() {
        override val displayText = "Wyjście"
        override val description = "Zamknij aplikację"
    }

    companion object {
        val allActions = listOf(NewDiagram, Exit)
    }
}
