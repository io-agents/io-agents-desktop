package com.pawlowski.io_agents_desktop.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pawlowski.io_agents_desktop.data.LLMCallInfo
import com.pawlowski.io_agents_desktop.data.WorkflowExecution
import com.pawlowski.io_agents_desktop.data.WorkflowNode
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class NodeLayout(
    val node: WorkflowNode,
    val row: Int,
    val col: Int,
)

@Composable
fun WorkflowVisualization(
    execution: WorkflowExecution,
    modifier: Modifier = Modifier,
) {
    var selectedNode by remember { mutableStateOf<WorkflowNode?>(null) }
    val nodeWidth = 120.dp
    val nodeHeight = 60.dp
    val horizontalSpacing = 40.dp
    val verticalSpacing = 100.dp
    val startX = 50.dp
    val startY = 50.dp
    val padding = 16.dp

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val colorScheme = MaterialTheme.colorScheme

    // Track available width
    var availableWidth by remember { mutableStateOf(0.dp) }

    // Calculate node layout based on available width
    // Use index as unique identifier for nodes (allows same node to appear multiple times)
    val nodeLayouts =
        remember(execution.nodes, availableWidth, nodeWidth, horizontalSpacing) {
            if (availableWidth <= 0.dp || execution.nodes.isEmpty()) {
                emptyList()
            } else {
                val nodeWithSpacing = nodeWidth + horizontalSpacing
                val maxNodesPerRow = ((availableWidth - startX - padding * 2) / nodeWithSpacing).toInt().coerceAtLeast(1)

                execution.nodes.mapIndexed { index, node ->
                    val row = index / maxNodesPerRow
                    val col = index % maxNodesPerRow
                    NodeLayout(node = node, row = row, col = col)
                }
            }
        }

    // Calculate actual canvas size based on layout
    val maxRow = nodeLayouts.maxOfOrNull { it.row } ?: 0
    val maxCol = nodeLayouts.maxOfOrNull { it.col } ?: 0
    val nodeWithSpacing = nodeWidth + horizontalSpacing
    val nodeHeightWithSpacing = nodeHeight + verticalSpacing
    // Canvas width: startX + nodes with spacing, but last node needs full width + padding on both sides
    val canvasWidthDp =
        if (nodeLayouts.isEmpty()) {
            startX + padding * 2
        } else {
            val calculatedWidth = startX + nodeWithSpacing * maxCol + nodeWidth + padding * 2
            if (availableWidth > 0.dp) {
                calculatedWidth.coerceAtLeast(availableWidth)
            } else {
                calculatedWidth
            }
        }
    // Canvas height: startY + rows with spacing, but last row needs full height + padding on both sides
    val canvasHeightDp =
        if (nodeLayouts.isEmpty()) {
            startY + padding * 2
        } else {
            // Last node position: startY + nodeHeightWithSpacing * maxRow
            // Last node height: nodeHeight
            // Bottom padding: padding
            // Total: startY + nodeHeightWithSpacing * maxRow + nodeHeight + padding * 2 (top and bottom)
            startY + nodeHeightWithSpacing * maxRow + nodeHeight + padding * 2
        }

    // Animate node appearances - track by node ID to preserve old animations
    val nodeAnimations = remember { mutableStateMapOf<String, Animatable<Float, AnimationVector1D>>() }

    // Track which nodes have been processed (to avoid re-animating)
    val processedNodeIds = remember { mutableSetOf<String>() }

    LaunchedEffect(execution.nodes.mapIndexed { index, node -> "${node.id}_$index" }) {
        // Use index-based unique keys to allow same node to appear multiple times
        execution.nodes.forEachIndexed { index, node ->
            val uniqueKey = "${node.id}_$index"

            // Ensure all nodes are in the animations map
            if (!nodeAnimations.containsKey(uniqueKey)) {
                val animatable = Animatable(0f)
                nodeAnimations[uniqueKey] = animatable
            }
        }

        // Then animate new nodes
        execution.nodes.forEachIndexed { index, node ->
            val uniqueKey = "${node.id}_$index"

            if (!processedNodeIds.contains(uniqueKey)) {
                processedNodeIds.add(uniqueKey)

                // Get or create animatable
                val animatable =
                    nodeAnimations[uniqueKey] ?: run {
                        val newAnimatable = Animatable(0f)
                        nodeAnimations[uniqueKey] = newAnimatable
                        newAnimatable
                    }

                // Stagger animation based on index
                delay(index * 200L)
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec =
                        tween(
                            durationMillis = 500,
                            easing = FastOutSlowInEasing,
                        ),
                )
            }
        }

        // Clean up animations for nodes that no longer exist
        val currentUniqueKeys = execution.nodes.mapIndexed { index, node -> "${node.id}_$index" }.toSet()
        nodeAnimations.keys.removeAll { it !in currentUniqueKeys }
        processedNodeIds.removeAll { it !in currentUniqueKeys }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Text(
                text = "Workflow Visualization",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp),
            )
        }

        // Scrollable canvas
        val verticalScrollState = rememberScrollState()
        val horizontalScrollState = rememberScrollState()

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .onSizeChanged { size ->
                        availableWidth = with(density) { size.width.toDp() }
                    },
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScrollState)
                        .horizontalScroll(horizontalScrollState),
            ) {
                Canvas(
                    modifier =
                        Modifier
                            .size(
                                width = canvasWidthDp,
                                height = canvasHeightDp,
                            ).pointerInput(nodeLayouts.size, availableWidth) {
                                detectTapGestures { tapOffset ->
                                    // Check if tap is on a node
                                    // Use same calculations as in drawing (with padding offset)
                                    val nodeWithSpacingPx = (nodeWidth + horizontalSpacing).toPx()
                                    val nodeHeightWithSpacingPx = (nodeHeight + verticalSpacing).toPx()
                                    val paddingPx = padding.toPx()

                                    nodeLayouts.forEach { layout ->
                                        val nodeX = (startX + padding).toPx() + nodeWithSpacingPx * layout.col
                                        val nodeY = (startY + padding).toPx() + nodeHeightWithSpacingPx * layout.row
                                        val nodeWidthPx = nodeWidth.toPx()
                                        val nodeHeightPx = nodeHeight.toPx()

                                        // Check if tap is within node bounds (rectangle check)
                                        val isWithinX = tapOffset.x >= nodeX && tapOffset.x <= nodeX + nodeWidthPx
                                        val isWithinY = tapOffset.y >= nodeY && tapOffset.y <= nodeY + nodeHeightPx

                                        if (isWithinX && isWithinY) {
                                            selectedNode = layout.node
                                        }
                                    }
                                }
                            },
                ) {
                    val nodeWidthPx = nodeWidth.toPx()
                    val nodeHeightPx = nodeHeight.toPx()

                    // Draw edges (arrows)
                    // Edges are created sequentially: edge[i] connects the last node before adding new node to the new node
                    // So we need to find the correct occurrence of each node
                    execution.edges.forEachIndexed { edgeIndex, edge ->
                        // Find the last occurrence of fromNodeId before this edge
                        var fromLayoutIndex = -1
                        for (i in edgeIndex downTo 0) {
                            if (execution.nodes[i].id == edge.fromNodeId) {
                                fromLayoutIndex = i
                                break
                            }
                        }

                        // Find the first occurrence of toNodeId after fromNodeId (should be at edgeIndex + 1)
                        var toLayoutIndex = -1
                        if (fromLayoutIndex >= 0) {
                            // The toNode should be the node that was added when this edge was created
                            // Edges are created when a new node is added, so toNode is at index = number of edges before this one + 1
                            val expectedToIndex = edgeIndex + 1
                            if (expectedToIndex < execution.nodes.size &&
                                execution.nodes[expectedToIndex].id == edge.toNodeId
                            ) {
                                toLayoutIndex = expectedToIndex
                            } else {
                                // Fallback: find first occurrence after fromNodeIndex
                                for (i in (fromLayoutIndex + 1) until execution.nodes.size) {
                                    if (execution.nodes[i].id == edge.toNodeId) {
                                        toLayoutIndex = i
                                        break
                                    }
                                }
                            }
                        }

                        if (fromLayoutIndex >= 0 && toLayoutIndex >= 0 &&
                            fromLayoutIndex < nodeLayouts.size && toLayoutIndex < nodeLayouts.size
                        ) {
                            val fromLayout = nodeLayouts[fromLayoutIndex]
                            val toLayout = nodeLayouts[toLayoutIndex]

                            val fromX = (startX + padding + nodeWithSpacing * fromLayout.col + nodeWidth / 2).toPx()
                            val fromY = (startY + padding + nodeHeightWithSpacing * fromLayout.row + nodeHeight / 2).toPx()
                            val toX = (startX + padding + nodeWithSpacing * toLayout.col + nodeWidth / 2).toPx()
                            val toY = (startY + padding + nodeHeightWithSpacing * toLayout.row + nodeHeight / 2).toPx()

                            // Get animation value for target node - use index-based key for unique identification
                            val toNodeUniqueKey = "${edge.toNodeId}_$toLayoutIndex"
                            val toAnimation = nodeAnimations[toNodeUniqueKey]?.value ?: 0f

                            // Only draw edge if target node has started animating (animation > 0)
                            if (toAnimation > 0f) {
                                // Animate edge drawing only if target node is animating (0 < animation < 1)
                                val currentToX =
                                    if (toAnimation < 1f) {
                                        fromX + (toX - fromX) * toAnimation
                                    } else {
                                        toX // Use final position when animation is complete
                                    }
                                val currentToY =
                                    if (toAnimation < 1f) {
                                        fromY + (toY - fromY) * toAnimation
                                    } else {
                                        toY // Use final position when animation is complete
                                    }

                                // Draw arrow line
                                val path =
                                    Path().apply {
                                        moveTo(fromX, fromY)
                                        lineTo(currentToX, currentToY)
                                    }

                                val edgeAlpha =
                                    if (toAnimation < 1f) {
                                        0.6f * toAnimation
                                    } else {
                                        0.6f
                                    }
                                drawPath(
                                    path = path,
                                    color = colorScheme.primary.copy(alpha = edgeAlpha),
                                    style = Stroke(width = 2.dp.toPx()),
                                )

                                // Draw arrowhead - draw if animation is at least 0.5 or complete
                                if (toAnimation >= 0.5f) {
                                    val angle = atan2(toY - fromY, toX - fromX)
                                    val arrowLength = 10.dp.toPx()
                                    val arrowAngle = 0.5f

                                    val arrowPath =
                                        Path().apply {
                                            moveTo(currentToX, currentToY)
                                            lineTo(
                                                currentToX - arrowLength * cos(angle - arrowAngle),
                                                currentToY - arrowLength * sin(angle - arrowAngle),
                                            )
                                            moveTo(currentToX, currentToY)
                                            lineTo(
                                                currentToX - arrowLength * cos(angle + arrowAngle),
                                                currentToY - arrowLength * sin(angle + arrowAngle),
                                            )
                                        }

                                    val arrowAlpha =
                                        if (toAnimation < 1f) {
                                            toAnimation
                                        } else {
                                            1f
                                        }
                                    drawPath(
                                        path = arrowPath,
                                        color = colorScheme.primary.copy(alpha = arrowAlpha),
                                        style = Stroke(width = 2.dp.toPx()),
                                    )
                                }
                            }
                        }
                    }

                    // Draw nodes - draw all nodes from execution to ensure none are missing
                    execution.nodes.forEachIndexed { index, node ->
                        // Find layout for this node by index (allows same node to appear multiple times)
                        val layout = nodeLayouts.getOrNull(index) ?: return@forEachIndexed

                        // Use index-based unique key for animation
                        val uniqueKey = "${node.id}_$index"
                        val animation = nodeAnimations[uniqueKey]?.value ?: 0f

                        // Draw node even if animation is 0 (it will be invisible but space is reserved)
                        // This ensures all nodes are accounted for in layout
                        val nodeX = (startX + padding + nodeWithSpacing * layout.col).toPx()
                        val nodeY = (startY + padding + nodeHeightWithSpacing * layout.row).toPx()

                        val alpha = animation
                        val scale = if (animation < 1f) 0.5f + 0.5f * animation else 1f

                        val width = nodeWidthPx * scale
                        val height = nodeHeightPx * scale
                        val centerX = nodeX + nodeWidthPx / 2
                        val centerY = nodeY + nodeHeightPx / 2

                        // Draw node background
                        drawRoundRect(
                            color = colorScheme.primaryContainer.copy(alpha = alpha),
                            topLeft = Offset(centerX - width / 2, centerY - height / 2),
                            size = Size(width, height),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                        )

                        // Draw node border
                        drawRoundRect(
                            color = colorScheme.primary.copy(alpha = alpha),
                            topLeft = Offset(centerX - width / 2, centerY - height / 2),
                            size = Size(width, height),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx()),
                        )

                        // Draw node text
                        val text = node.name
                        val textLayoutResult =
                            textMeasurer.measure(
                                text = text,
                                style =
                                    TextStyle(
                                        fontSize = 12.sp,
                                        color = colorScheme.onPrimaryContainer,
                                    ),
                                constraints = Constraints(maxWidth = width.toInt()),
                            )

                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft =
                                Offset(
                                    centerX - textLayoutResult.size.width / 2,
                                    centerY - textLayoutResult.size.height / 2,
                                ),
                            color = colorScheme.onPrimaryContainer.copy(alpha = alpha),
                        )

                        // Draw LLM call indicator if node has LLM calls
                        if (node.hasLLMCalls) {
                            val indicatorSize = 16.dp.toPx()
                            val indicatorX = centerX + width / 2 - indicatorSize / 2 - 4.dp.toPx()
                            val indicatorY = centerY - height / 2 + indicatorSize / 2 + 4.dp.toPx()

                            // Draw background circle for indicator
                            drawCircle(
                                color = Color(0xFFFFD700), // Gold color
                                radius = indicatorSize / 2,
                                center = Offset(indicatorX, indicatorY),
                                alpha = alpha,
                            )

                            // Draw border around indicator
                            drawCircle(
                                color = Color(0xFF000000), // Black border
                                radius = indicatorSize / 2,
                                center = Offset(indicatorX, indicatorY),
                                style = Stroke(width = 1.dp.toPx()),
                                alpha = alpha,
                            )

                            // Draw "AI" text
                            val indicatorText = "AI"
                            val indicatorTextLayout =
                                textMeasurer.measure(
                                    text = indicatorText,
                                    style =
                                        TextStyle(
                                            fontSize = 8.sp,
                                            color = Color.Black,
                                        ),
                                )
                            drawText(
                                textLayoutResult = indicatorTextLayout,
                                topLeft =
                                    Offset(
                                        indicatorX - indicatorTextLayout.size.width / 2,
                                        indicatorY - indicatorTextLayout.size.height / 2,
                                    ),
                                color = Color.Black.copy(alpha = alpha),
                            )
                        }
                    }
                }
            }
        }

        // LLM Call Details Dialog
        selectedNode?.let { node ->
            if (node.hasLLMCalls) {
                LLMCallDetailsDialog(
                    node = node,
                    onDismiss = { selectedNode = null },
                )
            } else {
                // Show info dialog for nodes without LLM calls
                AlertDialog(
                    onDismissRequest = { selectedNode = null },
                    title = { Text(text = node.name) },
                    text = { Text(text = "This node has no LLM calls.") },
                    confirmButton = {
                        TextButton(onClick = { selectedNode = null }) {
                            Text("OK")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun LLMCallDetailsDialog(
    node: WorkflowNode,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "LLM Calls: ${node.name}",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    IconButton(onClick = onDismiss) {
                        Text("✕", style = MaterialTheme.typography.titleLarge)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // LLM Calls List
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    node.llmCalls.forEachIndexed { index, call ->
                        LLMCallCard(
                            call = call,
                            callNumber = index + 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LLMCallCard(
    call: LLMCallInfo,
    callNumber: Int,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "LLM Call #$callNumber",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (expanded) "▼" else "▶",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))

                // Display prompt with better formatting (using structured data)
                // System prompt section
                if (call.systemMessages.isNotEmpty()) {
                    Text(
                        text = "System Prompt${if (call.systemMessages.size > 1) "s" else ""}:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    call.systemMessages.forEachIndexed { index, message ->
                        if (index > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            // Visual separator between multiple system prompts
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        // Add label for multiple system prompts
                        if (call.systemMessages.size > 1) {
                            Text(
                                text = "System Prompt #${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Surface(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 150.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .padding(12.dp),
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = message,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // User messages section
                if (call.userMessages.isNotEmpty()) {
                    Text(
                        text = "User Message${if (call.userMessages.size > 1) "s" else ""}:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    call.userMessages.forEachIndexed { index, message ->
                        if (index > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            // Visual separator between multiple user messages
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        // Add label for multiple user messages
                        if (call.userMessages.size > 1) {
                            Text(
                                text = "User Message #${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Surface(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 150.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .padding(12.dp),
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = message,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Response section
                if (call.isCompleted) {
                    Text(
                        text = "Response:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(12.dp),
                        ) {
                            SelectionContainer {
                                Text(
                                    text = call.response,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                } else {
                    // Show loading indicator if response is not yet available
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Waiting for response...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }
        }
    }
}
