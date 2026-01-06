package com.pawlowski.io_agents_desktop.di

import com.pawlowski.io_agents_desktop.data.AcceptanceRepository
import com.pawlowski.io_agents_desktop.data.AIAgentRepository
import com.pawlowski.io_agents_desktop.data.ClarificationRepository
import com.pawlowski.io_agents_desktop.data.SelectionRepository
import com.pawlowski.io_agents_desktop.data.WorkflowNodeTracker
import com.pawlowski.io_agents_desktop.domain.ChatUseCase
import com.pawlowski.io_agents_desktop.ui.ChatViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataModule = module {
    single { WorkflowNodeTracker() }
    singleOf(::AIAgentRepository)
    singleOf(::ClarificationRepository)
    singleOf(::AcceptanceRepository)
    singleOf(::SelectionRepository)
}

val domainModule = module {
    singleOf(::ChatUseCase)
}

val uiModule = module {
    single { ChatViewModel(get()) }
}

val appModule = module {
    includes(dataModule, domainModule, uiModule)
}

