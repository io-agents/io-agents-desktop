package com.pawlowski.io_agents_desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import org.koin.core.context.GlobalContext
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

@Composable
fun AiChat(
    modifier: Modifier = Modifier,
    onExit: () -> Unit = {},
) {
    val viewModel: ChatViewModel = remember { GlobalContext.get().get() }
    var state by remember { mutableStateOf(viewModel.state.value) }

    LaunchedEffect(Unit) {
        viewModel.state.collect { newState ->
            state = newState
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.dispose()
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        // Chat messages
        MessagesList(
            messages = state.messages,
            isLoading = state.isLoading,
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Next actions menu (shown when completed)
        if (state.isCompleted && state.availableNextActions.isNotEmpty()) {
            NextActionsMenu(
                actions = state.availableNextActions,
                onActionSelected = { action ->
                    if (action is NextAction.Exit) {
                        onExit()
                    } else {
                        viewModel.handleNextAction(action)
                    }
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Input field
        ChatInput(
            text = state.inputText,
            onTextChange = viewModel::updateInputText,
            onSendClick = viewModel::onSendClick,
            enabled = !state.isLoading && !state.isCompleted,
            placeholder =
                when {
                    state.isCompleted -> "Wybierz opcję powyżej..."
                    state.currentClarificationRequest != null -> "Odpowiedz na pytanie..."
                    state.currentAcceptanceRequest != null -> "Wpisz 'ACCEPT' lub poprawki..."
                    else -> "Napisz wiadomość..."
                },
        )
    }
}

@Composable
private fun MessagesList(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(messages) { message ->
            MessageBubble(message = message)
        }

        if (isLoading) {
            item {
                LoadingIndicator()
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.75f),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (message.isUser) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                ),
            shape =
                RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isUser) 16.dp else 4.dp,
                    bottomEnd = if (message.isUser) 4.dp else 16.dp,
                ),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
            ) {
                SelectionContainer {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                // Display diagram image if available
                message.diagramImagePath?.let { imagePath ->
                    val imageFile = File(imagePath)
                    if (imageFile.exists()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        DiagramImage(imagePath = imagePath)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagramImage(imagePath: String) {
    val imageBitmap: ImageBitmap? =
        remember(imagePath) {
            try {
                val file = File(imagePath)
                if (file.exists()) {
                    val bufferedImage: BufferedImage? = ImageIO.read(file)
                    bufferedImage?.let { img ->
                        val imageBytes = file.readBytes()
                        val image = Image.makeFromEncoded(imageBytes)
                        val bitmap = Bitmap.makeFromImage(image)
                        bitmap.asComposeImageBitmap()
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }

    imageBitmap?.let { bitmap ->
        androidx.compose.foundation.Image(
            bitmap = bitmap,
            contentDescription = "Use Case Diagram",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun LoadingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.2f),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
                Text(
                    text = "Myślę... 💭",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun NextActionsMenu(
    actions: List<NextAction>,
    onActionSelected: (NextAction) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Wybierz akcję:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        actions.forEach { action ->
            Card(
                onClick = { onActionSelected(action) },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = action.displayText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                    Text(
                        text = action.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    enabled: Boolean,
    placeholder: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(placeholder) },
            enabled = enabled,
            maxLines = 5,
            shape = RoundedCornerShape(24.dp),
        )

        FloatingActionButton(
            onClick = onSendClick,
            modifier = Modifier.size(56.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Text("→", style = MaterialTheme.typography.titleLarge)
        }
    }
}
