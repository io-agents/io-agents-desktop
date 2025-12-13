package com.pawlowski.io_agents_desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "io_agents_desktop",
    ) {
        App(onExit = ::exitApplication)
    }
}