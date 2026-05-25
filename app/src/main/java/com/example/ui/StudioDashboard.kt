package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TrackType
import com.example.ui.theme.*

@Composable
fun StudioDashboard(
    viewModel: StudioViewModel,
    onOpenAnimator: (Long) -> Unit
) {
    val projects by viewModel.projects.collectAsState()
    val currentProject by viewModel.currentProject.collectAsState()
    val scenes by viewModel.scenes.collectAsState()
    val tracks by viewModel.tracks.collectAsState()

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
            text = "UNIBLOX animate + uniblox visco",
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
    onOpenAnimator: (Long) -> Unit,
    onAddScene: () -> Unit
) {
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
        } else {
            // Show preview of current scene or list of scenes
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
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = White87,
                modifier = Modifier.size(64.dp)
            )
        }

        VerticalDivider(color = White60)

        // Tracks Area
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(5) { index -> // Fixed number of tracks for now
                val trackNumber = index + 1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .border(0.5.dp, White60),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$trackNumber",
                        color = White60,
                        modifier = Modifier.width(30.dp),
                        textAlign = TextAlign.Center
                    )
                    VerticalDivider(color = White60)
                    
                    // Track Content
                    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp), contentAlignment = Alignment.CenterStart) {
                        if (trackNumber == 1 && scenes.isNotEmpty()) {
                            TimelineClipView("scene 1", StudioAccent) {
                                (scenes.firstOrNull())?.id?.let { onOpenAnimator(it) }
                            }
                        } else if (trackNumber == 2) {
                            TimelineClipView("audio", StudioAudio) {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineClipView(label: String, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(120.dp)
            .height(28.dp)
            .clickable { onClick() },
        color = Color.Transparent,
        border = borderStroke(1.dp, Color.White),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = Color.White, fontSize = 12.sp)
            if (label == "audio") {
                Spacer(modifier = Modifier.weight(1f))
                Text("📊", fontSize = 10.sp) // Mock waveform
            }
        }
    }
}
