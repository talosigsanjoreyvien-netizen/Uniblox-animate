package `fun`.cybercode.simplyvisuals.uniblox_animate.ui

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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `fun`.cybercode.simplyvisuals.uniblox_animate.data.Point
import `fun`.cybercode.simplyvisuals.uniblox_animate.data.Stroke as DrawingStroke
import `fun`.cybercode.simplyvisuals.uniblox_animate.ui.theme.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class DrawingTool { PEN, MAGIC, ERASER, FILL }

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
        viewModel.startRecoverySession(sceneId, selectedIndex)
    }

    LaunchedEffect(selectedIndex) {
        viewModel.updateRecoverySession(selectedIndex)
    }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val defaultPenColor = if (isTablet) Color.Black else Color(0xFF444444) // Darker gray
    val defaultPenSize = if (isTablet) 8f else 6f

    var currentStrokes by remember { mutableStateOf(listOf<DrawingStroke>()) }
    var redoStrokes by remember { mutableStateOf(listOf<DrawingStroke>()) }
    var currentPoints = remember { mutableStateListOf<Point>() }
    var selectedColor by remember { mutableStateOf(defaultPenColor) }
    var strokeWidth by remember { mutableFloatStateOf(defaultPenSize) }
    var activeTool by remember { mutableStateOf(DrawingTool.PEN) }
    var onionSkinEnabled by remember { mutableStateOf(true) }
    var showColorPicker by remember { mutableStateOf(false) }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

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
                    viewModel.clearRecoverySession()
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

                VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp), color = White60)

                IconButton(onClick = { zoomScale *= 1.25f }) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.White)
                }
                IconButton(onClick = { zoomScale /= 1.25f }) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.White)
                }
                IconButton(onClick = { 
                    zoomScale = 1f
                    panOffset = Offset.Zero
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset Zoom", tint = Color.White)
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
                    ToolIcon(Icons.Default.Edit, activeTool == DrawingTool.PEN, "Pen") { 
                        activeTool = DrawingTool.PEN
                    }
                    ToolIcon(Icons.Default.AutoAwesome, activeTool == DrawingTool.MAGIC, "Magic") {
                        activeTool = DrawingTool.MAGIC
                    }
                    ToolIcon(Icons.Default.FormatColorFill, activeTool == DrawingTool.FILL, "Fill") {
                        activeTool = DrawingTool.FILL
                    }
                    ToolIcon(Icons.Default.AutoFixNormal, activeTool == DrawingTool.ERASER, "Eraser") { 
                        activeTool = DrawingTool.ERASER 
                    }
                    
                    Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 16.dp))

                    // Stroke Width Indicator/Slider Area
                    Text("Size", color = White60, fontSize = 10.sp)
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        listOf(4f, 8f, 16f, 32f).forEach { size ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (strokeWidth == size) StudioAccent else Color.Transparent)
                                    .border(1.dp, White60, CircleShape)
                                    .clickable { strokeWidth = size },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size((size / 2).coerceIn(2f, 18f).dp)
                                        .background(Color.White, CircleShape)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = strokeWidth,
                        onValueChange = { strokeWidth = it },
                        valueRange = 1f..64f,
                        modifier = Modifier.width(60.dp).graphicsLayer(rotationZ = -90f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(selectedColor, CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                            .clickable { showColorPicker = true }
                    )
                }

                // Canvas Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp)
                        .clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1.6f) // 16:10 for 1050p height
                            .graphicsLayer(
                                scaleX = zoomScale,
                                scaleY = zoomScale,
                                translationX = panOffset.x,
                                translationY = panOffset.y
                            )
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp)) // Correctly clip to rounded paper shape
                            .pointerInput(activeTool, selectedColor, strokeWidth) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentPoints.clear()
                                        currentPoints.add(Point(offset.x, offset.y))
                                    },
                                    onDrag = { change, _ ->
                                        currentPoints.add(Point(change.position.x, change.position.y))
                                    },
                                    onDragEnd = {
                                        val color = if (activeTool == DrawingTool.ERASER) Color.White else selectedColor
                                        val rawPoints = currentPoints.toList()
                                        val finalPoints = if (activeTool == DrawingTool.MAGIC) {
                                            perfectStroke(rawPoints)
                                        } else {
                                            rawPoints
                                        }
                                        
                                        // Auto-close for fill
                                        val pointsToSave = if (activeTool == DrawingTool.FILL && finalPoints.size > 2) {
                                            finalPoints + finalPoints.first()
                                        } else {
                                            finalPoints
                                        }

                                        val newStroke = DrawingStroke(
                                            points = pointsToSave,
                                            color = color.toArgb(),
                                            width = strokeWidth,
                                            isFill = activeTool == DrawingTool.FILL
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
                                    style = if (stroke.isFill) Fill else Stroke(
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
                                    color = if (activeTool == DrawingTool.ERASER) Color.White else selectedColor,
                                    style = if (activeTool == DrawingTool.FILL) Fill else Stroke(
                                        width = strokeWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
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
                        itemsIndexed(frames, key = { _, frame -> frame.id }) { index, frame ->
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
        if (showColorPicker) {
            ColorPickerDialog(
                initialColor = selectedColor,
                onDismiss = { showColorPicker = false },
                onColorSelected = { 
                    selectedColor = it
                    showColorPicker = false
                }
            )
        }
    }
}
}

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    var currentColor by remember { mutableStateOf(initialColor) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = StudioPanel,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Select Color", color = Color.White, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Color Wheel / Hue Slider and Saturation/Value Area
                ColorPickerBox(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    onColorChange = { currentColor = it }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(currentColor, CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                    )
                    
                    Column {
                        Text("R: ${(currentColor.red * 255).toInt()}", color = White60, fontSize = 12.sp)
                        Text("G: ${(currentColor.green * 255).toInt()}", color = White60, fontSize = 12.sp)
                        Text("B: ${(currentColor.blue * 255).toInt()}", color = White60, fontSize = 12.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = White60)
                    }
                    Button(
                        onClick = { onColorSelected(currentColor) },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioAccent)
                    ) {
                        Text("Select", color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun ColorPickerBox(
    modifier: Modifier = Modifier,
    onColorChange: (Color) -> Unit
) {
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var value by remember { mutableFloatStateOf(1f) }

    Column(modifier = modifier) {
        // Saturation-Value Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                        value = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                        onColorChange(Color.hsv(hue, saturation, value))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val hsvPaint = Paint().apply {
                    isAntiAlias = true
                }
                
                // Draw Saturation/Value gradient
                // This is a simplification. For a real one we'd need more complex gradients.
                drawRect(color = Color.hsv(hue, 1f, 1f))
                
                // Add Saturation and Value gradients
                val saturationOverlay = Brush.horizontalGradient(listOf(Color.White, Color.Transparent))
                drawRect(brush = saturationOverlay)
                
                val valueOverlay = Brush.verticalGradient(listOf(Color.Transparent, Color.Black))
                drawRect(brush = valueOverlay)
                
                // Selector circle
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(saturation * size.width, (1f - value) * size.height),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Hue Slider
        Box(
            modifier = Modifier
                .height(20.dp)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        hue = (change.position.x / size.width).coerceIn(0f, 360f)
                        onColorChange(Color.hsv(hue, saturation, value))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val hues = (0..360 step 60).map { Color.hsv(it.toFloat(), 1f, 1f) }
                drawRect(brush = Brush.horizontalGradient(hues))
                
                // Selector
                drawRect(
                    color = Color.White,
                    topLeft = Offset((hue / 360f) * size.width - 2.dp.toPx(), 0f),
                    size = size.copy(width = 4.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOnionFrame(frame: `fun`.cybercode.simplyvisuals.uniblox_animate.data.Frame, tint: Color) {
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
