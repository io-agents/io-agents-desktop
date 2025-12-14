package com.pawlowski.io_agents_desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.pawlowski.io_agents_desktop.di.appModule
import org.koin.core.context.startKoin

fun main() =
    application {
        startKoin {
            modules(appModule)
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = "io_agents_desktop",
        ) {
            App(onExit = ::exitApplication)
        }
    }
