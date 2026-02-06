package com.pawlowski.io_agents_desktop.domain.SAD

data class SADInput(
    val plainTextUseCaseDescription: String,
)

data class SADOutput(
    val scenariosActivitiesText: String,
)