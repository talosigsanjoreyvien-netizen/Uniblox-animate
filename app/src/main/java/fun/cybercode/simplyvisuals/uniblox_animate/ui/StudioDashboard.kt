package `fun`.cybercode.simplyvisuals.uniblox_animate.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke as DrawScopeStroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import android.os.Build
import `fun`.cybercode.simplyvisuals.uniblox_animate.data.*
import `fun`.cybercode.simplyvisuals.uniblox_animate.data.Stroke as DrawingStroke
import `fun`.cybercode.simplyvisuals.uniblox_animate.ui.theme.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.Json

@Composable
fun StudioDashboard(
    viewModel: StudioViewModel,
    onOpenAnimator: (Long) -> Unit
) {
    val projects by viewModel.projects.collectAsState()
    val currentProject by viewModel.currentProject.collectAsState()
    val scenes by viewModel.scenes.collectAsState()
    val tracks by viewModel.tracks.collectAsState()
    val clips by viewModel.clips.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackPositionMs by viewModel.playbackPositionMs.collectAsState()

    var selectedTrackId by remember { mutableStateOf<Long?>(null) }
    
    // Auto-select first track
    LaunchedEffect(tracks) {
        if (selectedTrackId == null && tracks.isNotEmpty()) {
            selectedTrackId = tracks.first().id
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioDark)
    ) {
        val isCompact = maxWidth < 600.dp
        val sidebarWidth = if (isCompact) 100.dp else 180.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Header
            DashboardHeader()

            Row(modifier = Modifier.weight(1f)) {
                // 2. Left Sidebar (Effects)
                if (!isCompact) {
                    EffectsSidebar(modifier = Modifier.width(sidebarWidth))
                }

                // 3. Main Content
                MainPreviewArea(
                    modifier = Modifier.weight(1f),
                    scenes = scenes,
                    tracks = tracks,
                    clips = clips,
                    isPlaying = isPlaying,
                    playbackPositionMs = playbackPositionMs,
                    viewModel = viewModel,
                    onOpenAnimator = onOpenAnimator,
                    onAddScene = { 
                        currentProject?.id?.let { viewModel.addScene(it, "scene ${scenes.size + 1}") }
                    }
                )

                // 4. Right Sidebar (Actions)
                ActionsSidebar(
                    modifier = Modifier.width(sidebarWidth),
                    selectedTrackId = selectedTrackId,
                    onAddScene = {
                        currentProject?.id?.let { viewModel.addScene(it, "scene ${scenes.size + 1}") }
                    },
                    onAddGif = { trackId ->
                        viewModel.addGif(trackId, "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExOHpzeXo3eXpzeXo3eXpzeXo3eXpzeXo3eXpzeXo3eXpzeXo3eXAmZXA9djFfaW50ZXJuYWxfZ2lmX2J5X2lkJmN0PWc/3o7TKVUn7iM8FMEU24/giphy.gif")
                    }
                )
            }

            // 5. Timeline
            TimelineArea(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompact) 150.dp else 200.dp),
                tracks = tracks,
                selectedTrackId = selectedTrackId,
                onSelectTrack = { selectedTrackId = it },
                scenes = scenes,
                clips = clips,
                isPlaying = isPlaying,
                playbackPositionMs = playbackPositionMs,
                onTogglePlayback = { viewModel.togglePlayback() },
                onSeek = { viewModel.seekTo(it) },
                onOpenAnimator = onOpenAnimator,
                onResizeClip = { id, duration -> viewModel.updateClipDuration(id, duration) }
            )
        }
    }
}

@Composable
fun DashboardHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Placeholder for the palette icon
        Surface(
            modifier = Modifier.size(24.dp),
            shape = RoundedCornerShape(4.dp),
            color = Color.White
        ) {
            Text("🎨", modifier = Modifier.wrapContentSize(), fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "UNIBLOX animate ultimate",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EffectsSidebar(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(8.dp)
            .border(1.dp, White60, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(
            text = "effects (powered by UNIBLOX visco)",
            color = Color.White,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "no effects yet",
                color = White60,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MainPreviewArea(
    modifier: Modifier = Modifier,
    scenes: List<Scene>,
    tracks: List<TimelineTrack>,
    clips: List<TimelineClip>,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    viewModel: StudioViewModel,
    onOpenAnimator: (Long) -> Unit,
    onAddScene: () -> Unit
) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 8.dp)
            .border(1.dp, White60, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (tracks.isEmpty()) {
            Text(
                text = "Adding tracks.....",
                color = Color.White
            )
        } else {
            // Render Background or Base
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
            ) {
                // Render Each Layer in Order
                tracks.indices.forEach { trackIndex ->
                    val track = tracks[trackIndex]
                    val activeClip = clips.find { it.trackId == track.id && playbackPositionMs >= it.startTimeMs && playbackPositionMs < it.startTimeMs + it.durationMs }
                    
                    if (activeClip != null) {
                        when (track.type) {
                            TrackType.SCENE -> {
                                activeClip.content.toLongOrNull()?.let { sceneId ->
                                    val framesState = remember(sceneId) { viewModel.getFrames(sceneId) }.collectAsState(initial = emptyList<Frame>())
                                    val frames = framesState.value
                                    if (frames.isNotEmpty()) {
                                        val timeInClip = playbackPositionMs - activeClip.startTimeMs
                                        val frameIndex = (timeInClip * StudioViewModel.FPS / 1000).toInt().coerceIn(0, frames.size - 1)
                                        FrameView(frames[frameIndex])
                                    }
                                }
                            }
                            TrackType.GIF -> {
                                AsyncImage(
                                    model = activeClip.content,
                                    contentDescription = "GIF Layer",
                                    imageLoader = imageLoader,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }

            // Play/Pause Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { viewModel.togglePlayback() },
                contentAlignment = Alignment.Center
            ) {
                if (!isPlaying && tracks.isNotEmpty() && clips.any { playbackPositionMs >= it.startTimeMs && playbackPositionMs < it.startTimeMs + it.durationMs }) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        color = Color.Black.copy(alpha = 0.3f),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp).padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FrameView(frame: Frame) {
    val strokes = remember(frame.strokesJson) {
        try {
            Json.decodeFromString<List<DrawingStroke>>(frame.strokesJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        strokes.forEach { stroke ->
            val path = Path().apply {
                if (stroke.points.isNotEmpty()) {
                    moveTo(stroke.points[0].x, stroke.points[0].y)
                    for (i in 1 until stroke.points.size) {
                        lineTo(stroke.points[i].x, stroke.points[i].y)
                    }
                }
            }
            val strokeWidth = stroke.width
            val strokeColor = stroke.color
            drawPath(
                path = path,
                color = Color(strokeColor),
                style = DrawScopeStroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@Composable
fun ActionsSidebar(
    modifier: Modifier = Modifier,
    selectedTrackId: Long?,
    onAddScene: () -> Unit,
    onAddGif: (Long) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(8.dp)
            .border(1.dp, White60, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionButton("add a scene", onAddScene)
        ActionButton("add a gif", { 
            selectedTrackId?.let { onAddGif(it) }
        })
        ActionButton("add a picture", {})
        
        if (selectedTrackId == null) {
            Text("select a layer first!", color = Color.Red, fontSize = 10.sp)
        }
    }
}

@Composable
fun ActionButton(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable { onClick() },
        color = Color.Transparent,
        border = borderStroke(1.dp, White60),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, color = Color.White, fontSize = 14.sp)
        }
    }
}

@Composable
fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)

@Composable
fun TimelineArea(
    modifier: Modifier = Modifier,
    tracks: List<TimelineTrack>,
    selectedTrackId: Long?,
    onSelectTrack: (Long) -> Unit,
    scenes: List<Scene>,
    clips: List<TimelineClip>,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    onTogglePlayback: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpenAnimator: (Long) -> Unit,
    onResizeClip: (Long, Long) -> Unit
) {
    Row(
        modifier = modifier
            .border(1.dp, White60)
            .padding(8.dp)
    ) {
        // Play Button Column
        Column(
            modifier = Modifier
                .width(100.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = onTogglePlayback,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = StudioAccent,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text(
                text = if (isPlaying) "STOP" else "PLAY",
                color = StudioAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        VerticalDivider(color = White60)

        // Tracks Area
        val scrollState = rememberScrollState()
        val timelineContentWidth = 2000.dp // Fixed width for now, or calculate based on duration

        Column(modifier = Modifier.weight(1f)) {
            // 1. Time Ruler (Interactive Seeking Area)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(White60.copy(alpha = 0.1f))
                    .horizontalScroll(scrollState)
            ) {
                Box(
                    modifier = Modifier
                        .width(timelineContentWidth)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                onSeek((offset.x.toDp().value * 10).toLong())
                            }
                        }
                ) {
                    // Time markers
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stepPx = 100.dp.toPx() // Every 1 second
                        for (i in 0..100) {
                            val x = i * stepPx
                            drawLine(
                                color = White87,
                                start = androidx.compose.ui.geometry.Offset(x, size.height * 0.6f),
                                end = androidx.compose.ui.geometry.Offset(x, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                            if (i % 5 == 0) {
                                // Larger marker every 5 seconds
                                drawLine(
                                    color = White87,
                                    start = androidx.compose.ui.geometry.Offset(x, size.height * 0.3f),
                                    end = androidx.compose.ui.geometry.Offset(x, size.height),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                        }
                    }
                }
            }

            // 2. Clips Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
                    .drawWithContent {
                        drawContent()
                        // Playhead (Visual Only - drawn over content)
                        val x = (playbackPositionMs / 10f).dp.toPx()
                        drawLine(
                            color = StudioAccent,
                            start = androidx.compose.ui.geometry.Offset(x, 0f),
                            end = androidx.compose.ui.geometry.Offset(x, size.height),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
            ) {
                Column(modifier = Modifier.width(timelineContentWidth).fillMaxHeight()) {
                    tracks.forEach { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(if (selectedTrackId == track.id) StudioAccent.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { onSelectTrack(track.id) }
                                .border(0.5.dp, White60),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${tracks.indexOf(track) + 1}",
                                color = White60,
                                modifier = Modifier.width(30.dp),
                                textAlign = TextAlign.Center
                            )
                            VerticalDivider(color = White60)
                            
                            // Track Content
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                                val trackClips = clips.filter { it.trackId == track.id }
                                trackClips.forEach { clip ->
                                    val sceneName = scenes.find { it.id.toString() == clip.content }?.name ?: "scene"
                                    TimelineClipView(
                                        label = sceneName,
                                        color = if (track.type == TrackType.SCENE) StudioAccent else StudioAudio,
                                        modifier = Modifier
                                            .padding(start = (clip.startTimeMs / 10f).dp)
                                            .width((clip.durationMs / 10f).dp),
                                        onResize = { deltaMs ->
                                            val newDuration = (clip.durationMs + deltaMs).coerceAtLeast(100L)
                                            onResizeClip(clip.id, newDuration)
                                        }
                                    ) {
                                        if (track.type == TrackType.SCENE) {
                                            clip.content.toLongOrNull()?.let { onOpenAnimator(it) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineClipView(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onResize: ((Long) -> Unit)? = null,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    var accumulatedDeltaPx by remember { mutableFloatStateOf(0f) }

    Surface(
        modifier = modifier
            .height(36.dp),
        color = color.copy(alpha = 0.4f),
        border = borderStroke(2.dp, color),
        shape = RoundedCornerShape(6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onClick() }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Right Drag Handle
            if (onResize != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(12.dp)
                        .background(color.copy(alpha = 0.3f))
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { accumulatedDeltaPx = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    accumulatedDeltaPx += dragAmount
                                    // 1dp = 10ms (based on our 10ms per dp scaling)
                                    val deltaMs = (accumulatedDeltaPx / density.density * 10).toLong()
                                    if (deltaMs != 0L) {
                                        onResize(deltaMs)
                                        // Reset accumulated delta if we triggered a change (approx)
                                        // But since we want smooth resizing, we might just keep it or reset based on consumed amount
                                        // Simple way: reset if deltaMs is non-zero
                                        accumulatedDeltaPx = 0f
                                    }
                                }
                            )
                        }
                ) {
                    // Small visual indicator for handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(2.dp)
                            .height(16.dp)
                            .background(Color.White.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}
