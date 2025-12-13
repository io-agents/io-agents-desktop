package com.pawlowski.io_agents_desktop.domain.useCase

data class UseCaseDiagramInput(
    val plainTextUseCaseDescription: String,
)

data class UseCaseDiagramOutput(
    val plantUmlText: String,
)

