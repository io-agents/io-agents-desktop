package com.pawlowski.io_agents_desktop.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pawlowski.io_agents_desktop.data.WorkflowEdge
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
    val nodeLayouts = remember(execution.nodes, availableWidth, nodeWidth, horizontalSpacing) {
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
    val canvasWidthDp = if (availableWidth > 0.dp) {
        (startX + nodeWithSpacing * (maxCol + 1) + padding).coerceAtLeast(availableWidth)
    } else {
        startX + nodeWithSpacing * (maxCol + 1) + padding
    }
    val canvasHeightDp = startY + nodeHeightWithSpacing * (maxRow + 1) + padding
    
    // Animate node appearances - track by node ID to preserve old animations
    val nodeAnimations = remember { mutableStateMapOf<String, Animatable<Float, AnimationVector1D>>() }
    
    // Track which nodes have been processed (to avoid re-animating)
    val processedNodeIds = remember { mutableSetOf<String>() }
    
    LaunchedEffect(execution.nodes) {
        execution.nodes.forEach { node ->
            if (!processedNodeIds.contains(node.id)) {
                // New node - initialize with 0f and add to map immediately
                val animatable = Animatable(0f)
                nodeAnimations[node.id] = animatable
                processedNodeIds.add(node.id)
                
                // Find index for stagger delay
                val index = execution.nodes.indexOfFirst { it.id == node.id }
                delay(index * 200L) // Stagger animation
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 500,
                        easing = FastOutSlowInEasing,
                    ),
                )
            }
        }
        
        // Clean up animations for nodes that no longer exist (shouldn't happen, but just in case)
        val currentNodeIds = execution.nodes.map { it.id }.toSet()
        nodeAnimations.keys.removeAll { it !in currentNodeIds }
        processedNodeIds.removeAll { it !in currentNodeIds }
    }
    
    Column(
        modifier = modifier
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
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .onSizeChanged { size ->
                    availableWidth = with(density) { size.width.toDp() }
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScrollState)
                    .horizontalScroll(horizontalScrollState),
            ) {
                Canvas(
                    modifier = Modifier
                        .size(
                            width = canvasWidthDp,
                            height = canvasHeightDp,
                        )
                        .padding(padding),
                ) {
                    val nodeWidthPx = nodeWidth.toPx()
                    val nodeHeightPx = nodeHeight.toPx()
                    
                    // Draw edges (arrows)
                    execution.edges.forEach { edge ->
                        val fromLayoutIndex = nodeLayouts.indexOfFirst { it.node.id == edge.fromNodeId }
                        val toLayoutIndex = nodeLayouts.indexOfFirst { it.node.id == edge.toNodeId }
                        
                        if (fromLayoutIndex >= 0 && toLayoutIndex >= 0) {
                            val fromLayout = nodeLayouts[fromLayoutIndex]
                            val toLayout = nodeLayouts[toLayoutIndex]
                            
                            val fromX = (startX + nodeWithSpacing * fromLayout.col + nodeWidth / 2).toPx()
                            val fromY = (startY + nodeHeightWithSpacing * fromLayout.row + nodeHeight / 2).toPx()
                            val toX = (startX + nodeWithSpacing * toLayout.col + nodeWidth / 2).toPx()
                            val toY = (startY + nodeHeightWithSpacing * toLayout.row + nodeHeight / 2).toPx()
                            
                            // Get animation value - if node is not in map, it's new and should start at 0f
                            val toAnimation = nodeAnimations[edge.toNodeId]?.value ?: 0f
                            
                            // Always draw the edge, but animate if the target node is still animating
                            if (toAnimation > 0f) {
                                // Animate edge drawing for new nodes
                                val currentToX = if (toAnimation < 1f) {
                                    fromX + (toX - fromX) * toAnimation
                                } else {
                                    toX
                                }
                                val currentToY = if (toAnimation < 1f) {
                                    fromY + (toY - fromY) * toAnimation
                                } else {
                                    toY
                                }
                                
                                // Draw arrow line
                                val path = Path().apply {
                                    moveTo(fromX, fromY)
                                    lineTo(currentToX, currentToY)
                                }
                                
                                val edgeAlpha = if (toAnimation < 1f) 0.6f * toAnimation else 0.6f
                                drawPath(
                                    path = path,
                                    color = colorScheme.primary.copy(alpha = edgeAlpha),
                                    style = Stroke(width = 2.dp.toPx()),
                                )
                                
                                // Draw arrowhead (always draw if animation is complete or if we're animating)
                                if (toAnimation >= 1f || toAnimation > 0.5f) {
                                    val angle = atan2(toY - fromY, toX - fromX)
                                    val arrowLength = 10.dp.toPx()
                                    val arrowAngle = 0.5f
                                    
                                    val arrowPath = Path().apply {
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
                                    
                                    drawPath(
                                        path = arrowPath,
                                        color = colorScheme.primary.copy(alpha = if (toAnimation < 1f) toAnimation else 1f),
                                        style = Stroke(width = 2.dp.toPx()),
                                    )
                                }
                            }
                        }
                    }
                    
                    // Draw nodes
                    nodeLayouts.forEach { layout ->
                        // Get animation value - if node is not in map, it's new and should start at 0f
                        val animation = nodeAnimations[layout.node.id]?.value ?: 0f
                        
                        // Only draw nodes that have started animating (animation > 0f)
                        if (animation > 0f) {
                            val nodeX = (startX + nodeWithSpacing * layout.col).toPx()
                            val nodeY = (startY + nodeHeightWithSpacing * layout.row).toPx()
                            
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
                            val text = layout.node.name
                            val textLayoutResult = textMeasurer.measure(
                                text = text,
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = colorScheme.onPrimaryContainer,
                                ),
                                constraints = Constraints(maxWidth = width.toInt()),
                            )
                            
                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(
                                    centerX - textLayoutResult.size.width / 2,
                                    centerY - textLayoutResult.size.height / 2,
                                ),
                                color = colorScheme.onPrimaryContainer.copy(alpha = alpha),
                            )
                        }
                    }
                }
            }
        }
    }
}

