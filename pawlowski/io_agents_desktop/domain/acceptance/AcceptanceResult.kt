package com.pawlowski.io_agents_desktop.domain.acceptance

data class AcceptanceResult<Input>(
    val response: Input,
    val accepted: Boolean,
    val correctionsNeeded: String?,
)

