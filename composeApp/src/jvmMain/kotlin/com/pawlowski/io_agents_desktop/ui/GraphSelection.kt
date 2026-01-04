package com.pawlowski.io_agents_desktop.ui

/**
 * Represents algorithm starting poits selection.
 */
sealed class StartGraph {
    abstract val displayText: String
    abstract val description: String

    object UCD : StartGraph() {
        override val displayText = "UCD"
        override val description = "Stwórz diagram przypadków użycia"
    }

    object SAD : StartGraph() {
        override val displayText = "SAD"
        override val description = "Wyciągnij scenariusze i aktywności z diagramu przypadków użycia"
    }

    object ADM : StartGraph() {
        override val displayText = "ADM"
        override val description = "Stwórz diagram aktywności na podstawie scenariuszy i aktywności"
    }

    companion object {
        val allActions = listOf(UCD, SAD, ADM)
    }
}
