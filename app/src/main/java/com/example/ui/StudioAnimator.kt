package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Point
import com.example.data.Stroke as DrawingStroke
import com.example.ui.theme.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
fun StudioAnimator(
    viewModel: StudioViewModel,
    sceneId: Long,
    onBack: () -> Unit
) {
    val frames by viewModel.currentFrames.collectAsState()
    val selectedIndex by viewModel.selectedFrameIndex.collectAsState()

    LaunchedEffect(sceneId) {
        viewModel.selectScene(sceneId)
    }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val defaultPenColor = if (isTablet) Color.Black else Color(0xFF444444) // Darker gray

    var currentStrokes by remember { mutableStateOf(listOf<DrawingStroke>()) }
    var redoStrokes by remember { mutableStateOf(listOf<DrawingStroke>()) }
    var currentPoints = remember { mutableStateListOf<Point>() }
    var selectedColor by remember { mutableStateOf(defaultPenColor) }
    var strokeWidth by remember { mutableFloatStateOf(5f) }
    var isEraser by remember { mutableStateOf(false) }
    var isPerfecting by remember { mutableStateOf(true) } // Default to ON
    var onionSkinEnabled by remember { mutableStateOf(true) }

    // Load strokes when frame changes
    LaunchedEffect(selectedIndex, frames) {
        if (selectedIndex < frames.size) {
            val strokesJson = frames[selectedIndex].strokesJson
            try {
                currentStrokes = Json.decodeFromString(strokesJson)
            } catch (e: Exception) {
                currentStrokes = emptyList()
            }
        }
    }

    // Save strokes periodically or on change
    val saveStrokes = {
        val json = Json.encodeToString(currentStrokes)
        viewModel.saveFrame(json)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioDark)
    ) {
        val isCompact = maxWidth < 600.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { 
                    saveStrokes()
                    onBack() 
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("Animator", color = Color.White, fontSize = if (isCompact) 16.sp else 20.sp)
                Spacer(modifier = Modifier.weight(1f))
                
                IconButton(onClick = { 
                    if (currentStrokes.isNotEmpty()) {
                        redoStrokes = redoStrokes + currentStrokes.last()
                        currentStrokes = currentStrokes.dropLast(1)
                        saveStrokes()
                    }
                }) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo", tint = Color.White)
                }
                IconButton(onClick = {
                    if (redoStrokes.isNotEmpty()) {
                        currentStrokes = currentStrokes + redoStrokes.last()
                        redoStrokes = redoStrokes.dropLast(1)
                        saveStrokes()
                    }
                }) {
                    Icon(Icons.Default.Redo, contentDescription = "Redo", tint = Color.White)
                }

                IconButton(onClick = { onionSkinEnabled = !onionSkinEnabled }) {
                    Icon(
                        imageVector = if (onionSkinEnabled) Icons.Default.Layers else Icons.Default.LayersClear,
                        contentDescription = "Onion Skin",
                        tint = if (onionSkinEnabled) StudioAccent else Color.White
                    )
                }
            }

            Row(modifier = Modifier.weight(1f)) {
                // Tool Sidebar
                Column(
                    modifier = Modifier
                        .width(if (isCompact) 50.dp else 60.dp)
                        .fillMaxHeight()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ToolIcon(Icons.Default.Edit, !isEraser && !isPerfecting, "Pen") { 
                        isEraser = false
                        isPerfecting = false
                    }
                    ToolIcon(Icons.Default.AutoAwesome, isPerfecting && !isEraser, "Magic") {
                        isEraser = false
                        isPerfecting = true
                    }
                    ToolIcon(Icons.Default.AutoFixNormal, isEraser, "Eraser") { isEraser = true }
                    
                    Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 16.dp))

                    // Stroke Width Indicator/Slider Area (Vertical slider would be better, but horizontal for now)
                    Text("Size", color = White60, fontSize = 10.sp)
                    Slider(
                        value = strokeWidth,
                        onValueChange = { strokeWidth = it },
                        valueRange = 1f..50f,
                        modifier = Modifier.width(40.dp).graphicsLayer(rotationZ = -90f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    ColorCircle(Color.Black, selectedColor == Color.Black) { selectedColor = Color.Black }
                    ColorCircle(Color.Gray, selectedColor == Color.Gray) { selectedColor = Color.Gray }
                    ColorCircle(Color.White, selectedColor == Color.White) { selectedColor = Color.White }
                    ColorCircle(Color.Red, selectedColor == Color.Red) { selectedColor = Color.Red }
                    ColorCircle(Color.Green, selectedColor == Color.Green) { selectedColor = Color.Green }
                    ColorCircle(Color.Blue, selectedColor == Color.Blue) { selectedColor = Color.Blue }
                }

                // Canvas Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp)) // Correctly clip to rounded paper shape
                        .pointerInput(isEraser, selectedColor, strokeWidth) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPoints.clear()
                                    currentPoints.add(Point(offset.x, offset.y))
                                },
                                onDrag = { change, _ ->
                                    currentPoints.add(Point(change.position.x, change.position.y))
                                },
                                onDragEnd = {
                                    val color = if (isEraser) Color.White else selectedColor
                                    val rawPoints = currentPoints.toList()
                                    val finalPoints = if (isPerfecting && !isEraser) {
                                        perfectStroke(rawPoints)
                                    } else {
                                        rawPoints
                                    }
                                    
                                    val newStroke = DrawingStroke(
                                        points = finalPoints,
                                        color = color.toArgb(),
                                        width = strokeWidth
                                    )
                                    currentStrokes = currentStrokes + newStroke
                                    redoStrokes = emptyList()
                                    currentPoints.clear()
                                    saveStrokes()
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (onionSkinEnabled) {
                            // Onion Skin (Previous Frame - Red tint)
                            if (selectedIndex > 0 && selectedIndex - 1 < frames.size) {
                                drawOnionFrame(frames[selectedIndex - 1], Color.Red.copy(alpha = 0.15f))
                            }
                            // Onion Skin (Next Frame - Green tint)
                            if (selectedIndex + 1 < frames.size) {
                                drawOnionFrame(frames[selectedIndex + 1], Color.Green.copy(alpha = 0.15f))
                            }
                        }

                        // Current Strokes
                        currentStrokes.forEach { stroke ->
                            val path = Path().apply {
                                if (stroke.points.isNotEmpty()) {
                                    moveTo(stroke.points[0].x, stroke.points[0].y)
                                    for (i in 1 until stroke.points.size) {
                                        lineTo(stroke.points[i].x, stroke.points[i].y)
                                    }
                                }
                            }
                            drawPath(
                                path = path,
                                color = Color(stroke.color),
                                style = Stroke(
                                    width = stroke.width,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        // In-progress Stroke
                        if (currentPoints.isNotEmpty()) {
                            val path = Path().apply {
                                moveTo(currentPoints[0].x, currentPoints[0].y)
                                for (i in 1 until currentPoints.size) {
                                    lineTo(currentPoints[i].x, currentPoints[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = if (isEraser) Color.White else selectedColor,
                                style = Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
            }

            // Frame Navigator
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompact) 80.dp else 100.dp),
                color = StudioPanel
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(frames) { index, frame ->
                            FrameThumb(
                                index = index,
                                isSelected = index == selectedIndex,
                                isCompact = isCompact,
                                onClick = { 
                                    saveStrokes()
                                    viewModel.selectFrame(index) 
                                }
                            )
                        }
                    }
                    
                    IconButton(onClick = { 
                        saveStrokes()
                        viewModel.addFrame() 
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Frame", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ToolIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(if (isSelected) 40.dp else 36.dp)
                .background(if (isSelected) StudioAccent else Color.Transparent, CircleShape)
                .border(1.dp, White60, CircleShape)
        ) {
            Icon(icon, contentDescription = label, tint = if (isSelected) Color.Black else Color.White)
        }
        Text(text = label, color = White60, fontSize = 8.sp)
    }
}

@Composable
fun ColorCircle(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(color, CircleShape)
            .border(if (isSelected) 3.dp else 1.dp, if (isSelected) StudioAccent else White60, CircleShape)
            .clickable { onClick() }
    )
}

@Composable
fun FrameThumb(index: Int, isSelected: Boolean, isCompact: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(if (isCompact) 48.dp else 60.dp, if (isCompact) 64.dp else 80.dp)
            .background(if (isSelected) StudioAccent else Color.Gray, RoundedCornerShape(4.dp))
            .border(2.dp, if (isSelected) Color.White else Color.Transparent, RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = "${index + 1}", color = Color.White)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOnionFrame(frame: com.example.data.Frame, tint: Color) {
    try {
        val strokes: List<DrawingStroke> = Json.decodeFromString(frame.strokesJson)
        strokes.forEach { stroke ->
            val path = Path().apply {
                if (stroke.points.isNotEmpty()) {
                    moveTo(stroke.points[0].x, stroke.points[0].y)
                    for (i in 1 until stroke.points.size) {
                        lineTo(stroke.points[i].x, stroke.points[i].y)
                    }
                }
            }
            drawPath(
                path = path,
                color = tint,
                style = Stroke(
                    width = stroke.width,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    } catch (e: Exception) {}
}

fun Color.toArgb(): Int = (this.alpha * 255).toInt() shl 24 or
        ((this.red * 255).toInt() shl 16) or
        ((this.green * 255).toInt() shl 8) or
        (this.blue * 255).toInt()

private fun perfectStroke(points: List<Point>): List<Point> {
    if (points.size < 5) return points

    // 1. Line Straightening logic
    val start = points.first()
    val end = points.last()
    
    // Calculate total path length
    var totalLength = 0.0
    for (i in 0 until points.size - 1) {
        val dx = points[i+1].x - points[i].x
        val dy = points[i+1].y - points[i].y
        totalLength += Math.sqrt((dx * dx + dy * dy).toDouble())
    }
    
    // Calculate straight line distance
    val ldx = end.x - start.x
    val ldy = end.y - start.y
    val linearDist = Math.sqrt((ldx * ldx + ldy * ldy).toDouble())

    // If the path is nearly straight, snap it to a line
    if (linearDist > 0 && (totalLength / linearDist) < 1.1) {
        return listOf(start, end)
    }

    // 2. Curves Perfecting logic (Laplacian Smoothing)
    val smoothed = mutableListOf<Point>()
    smoothed.add(points.first())
    
    // Iterative smoothing (2 passes for "perfect" curves)
    var currentPass = points
    repeat(2) {
        val nextPass = mutableListOf<Point>()
        nextPass.add(currentPass.first())
        for (i in 1 until currentPass.size - 1) {
            val prev = currentPass[i - 1]
            val curr = currentPass[i]
            val next = currentPass[i + 1]
            // Weighted average
            nextPass.add(Point(
                x = (prev.x + curr.x * 2f + next.x) / 4f,
                y = (prev.y + curr.y * 2f + next.y) / 4f
            ))
        }
        nextPass.add(currentPass.last())
        currentPass = nextPass
    }
    
    return currentPass
}
