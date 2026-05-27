package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Frame
import com.example.data.TimelineClip
import com.example.data.TrackType
import com.example.ui.theme.*
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

    // Auto-create/select project if none
    LaunchedEffect(projects) {
        if (projects.isEmpty()) {
            viewModel.createProject("My First Project")
        } else if (currentProject == null) {
            viewModel.selectProject(projects.first().id)
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
                    clips = clips,
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
                    onAddScene = {
                        currentProject?.id?.let { viewModel.addScene(it, "scene ${scenes.size + 1}") }
                    }
                )
            }

            // 5. Timeline
            TimelineArea(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompact) 150.dp else 200.dp),
                tracks = tracks,
                scenes = scenes,
                clips = clips,
                isPlaying = isPlaying,
                playbackPositionMs = playbackPositionMs,
                onTogglePlayback = { viewModel.togglePlayback() },
                onSeek = { viewModel.seekTo(it) },
                onOpenAnimator = onOpenAnimator
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
    scenes: List<com.example.data.Scene>,
    clips: List<TimelineClip>,
    playbackPositionMs: Long,
    viewModel: StudioViewModel,
    onOpenAnimator: (Long) -> Unit,
    onAddScene: () -> Unit
) {
    val activeClip = clips.find { playbackPositionMs >= it.startTimeMs && playbackPositionMs < it.startTimeMs + it.durationMs }
    val sceneId = activeClip?.content?.toLongOrNull()
    
    val currentFramesState = remember(sceneId) {
        if (sceneId != null) viewModel.getFrames(sceneId) else flowOf(emptyList<Frame>())
    }.collectAsState(initial = emptyList<Frame>())

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 8.dp)
            .border(1.dp, White60, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (scenes.isEmpty()) {
            Text(
                text = "click a scene to open the animator\nor add a scene.....",
                color = Color.White,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp,
                modifier = Modifier.clickable { onAddScene() }
            )
        } else if (activeClip != null && sceneId != null) {
            val frames = currentFramesState.value
            if (frames.isNotEmpty()) {
                val timeInClip = playbackPositionMs - activeClip.startTimeMs
                val progress = timeInClip.toFloat() / activeClip.durationMs
                val frameIndex = (progress * frames.size).toInt().coerceIn(0, frames.size - 1)
                
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    FrameView(frames[frameIndex])
                }

                Box(modifier = Modifier.fillMaxSize().clickable { onOpenAnimator(sceneId) })
            } else {
                CircularProgressIndicator(color = StudioAccent)
            }
        } else {
            Text(
                text = "Project contains ${scenes.size} scenes.\nClick a scene below to edit.",
                color = Color.White,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable { 
                    scenes.firstOrNull()?.id?.let { onOpenAnimator(it) }
                }
            )
        }
    }
}

@Composable
fun FrameView(frame: Frame) {
    val strokes = remember(frame.strokesJson) {
        try {
            Json.decodeFromString<List<com.example.data.Stroke>>(frame.strokesJson)
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
    }
}

@Composable
fun ActionsSidebar(
    modifier: Modifier = Modifier,
    onAddScene: () -> Unit
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
        ActionButton("add a gif", {})
        ActionButton("add a picture", {})
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
    tracks: List<com.example.data.TimelineTrack>,
    scenes: List<com.example.data.Scene>,
    clips: List<TimelineClip>,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    onTogglePlayback: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpenAnimator: (Long) -> Unit
) {
    Row(
        modifier = modifier
            .border(1.dp, White60)
            .padding(8.dp)
    ) {
        // Play Button Column
        Column(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = onTogglePlayback) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = White87,
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        VerticalDivider(color = White60)

        // Tracks Area
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(tracks) { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
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
                        Box(modifier = Modifier.fillMaxSize()) {
                            val trackClips = clips.filter { it.trackId == track.id }
                            trackClips.forEach { clip ->
                                val sceneName = scenes.find { it.id.toString() == clip.content }?.name ?: "scene"
                                TimelineClipView(
                                    label = sceneName,
                                    color = if (track.type == TrackType.SCENE) StudioAccent else StudioAudio,
                                    modifier = Modifier
                                        .padding(start = (clip.startTimeMs / 10f).dp)
                                        .width((clip.durationMs / 10f).dp)
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

            // Playhead
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            onSeek((offset.x.toDp().value * 10).toLong())
                        }
                    }
            ) {
                val x = (playbackPositionMs / 10f).dp.toPx()
                drawLine(
                    color = StudioAccent,
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

@Composable
fun TimelineClipView(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(28.dp)
            .background(color.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .clickable { onClick() },
        color = Color.Transparent,
        border = borderStroke(1.dp, color),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = Color.White, fontSize = 10.sp, maxLines = 1)
        }
    }
}
