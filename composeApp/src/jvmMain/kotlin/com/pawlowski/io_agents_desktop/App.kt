package com.pawlowski.io_agents_desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pawlowski.io_agents_desktop.ui.AiChat
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(onExit: () -> Unit = {}) {
    MaterialTheme {
        AiChat(
            modifier = Modifier.fillMaxSize(),
            onExit = onExit,
        )
    }
}
