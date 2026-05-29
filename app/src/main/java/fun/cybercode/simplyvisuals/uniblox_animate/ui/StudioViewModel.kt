package `fun`.cybercode.simplyvisuals.uniblox_animate.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import `fun`.cybercode.simplyvisuals.uniblox_animate.data.*
import `fun`.cybercode.simplyvisuals.uniblox_animate.service.RecoveryService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val FPS = 12
    }
    
    private val db = Room.databaseBuilder(
        application,
        StudioDatabase::class.java, "studio-db"
    ).fallbackToDestructiveMigration().build()

    private val dao = db.studioDao()

    val recoverySession = flow {
        emit(dao.getRecoverySession())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun startRecoverySession(sceneId: Long, frameIndex: Int) {
        val projectId = currentProjectId.value ?: return
        viewModelScope.launch {
            dao.saveRecoverySession(RecoverySession(projectId = projectId, sceneId = sceneId, frameIndex = frameIndex))
        }
    }

    fun updateRecoverySession(frameIndex: Int) {
        val session = recoverySession.value ?: return
        viewModelScope.launch {
            dao.saveRecoverySession(session.copy(frameIndex = frameIndex, timestamp = System.currentTimeMillis()))
        }
    }

    fun clearRecoverySession() {
        viewModelScope.launch {
            dao.clearRecoverySession()
        }
    }

    fun checkAndShowRecovery(context: android.content.Context) {
        viewModelScope.launch {
            val session = dao.getRecoverySession()
            if (session != null) {
                // Determine if it was "accidental" - e.g. timestamp is old or we just assume if it exists.
                // Start the service to show the overlay
                val intent = android.content.Intent(context, RecoveryService::class.java)
                if (android.provider.Settings.canDrawOverlays(context)) {
                    context.startService(intent)
                }
            }
        }
    }

    val projects = dao.getAllProjects().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _currentProjectId = MutableStateFlow<Long?>(null)
    val currentProjectId = _currentProjectId.asStateFlow()

    val currentProject = _currentProjectId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else projects.map { it.find { p -> p.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val scenes = _currentProjectId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else dao.getScenesByProject(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tracks = _currentProjectId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else dao.getTracksByProject(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clips = tracks.flatMapLatest { trackList ->
        if (trackList.isEmpty()) flowOf(emptyList<TimelineClip>())
        else {
            val flows = trackList.map { dao.getClipsByTrack(it.id) }
            combine(flows) { it.flatMap { list -> list as List<TimelineClip> } }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs = _playbackPositionMs.asStateFlow()

    private var playbackJob: kotlinx.coroutines.Job? = null

    fun togglePlayback() {
        _isPlaying.value = !_isPlaying.value
        if (_isPlaying.value) {
            startPlayback()
        } else {
            playbackJob?.cancel()
        }
    }

    private fun startPlayback() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis() - _playbackPositionMs.value
            while (true) {
                _playbackPositionMs.value = System.currentTimeMillis() - startTime
                // Basic loop logic: if we exceed max duration, reset to 0
                val maxDuration = clips.value.maxOfOrNull { it.startTimeMs + it.durationMs } ?: 0L
                if (maxDuration > 0 && _playbackPositionMs.value >= maxDuration) {
                    _playbackPositionMs.value = 0
                    startPlayback()
                    break
                }
                kotlinx.coroutines.delay(1000L / FPS)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        _playbackPositionMs.value = positionMs
        if (!_isPlaying.value) {
            playbackJob?.cancel()
        }
    }

    fun selectProject(id: Long) {
        _currentProjectId.value = id
    }

    fun createProject(name: String) {
        viewModelScope.launch {
            val id = dao.insertProject(Project(name = name))
            _currentProjectId.value = id
            // Add default tracks
            dao.insertTrack(TimelineTrack(projectId = id, name = "Layer 1", type = TrackType.SCENE))
            dao.insertTrack(TimelineTrack(projectId = id, name = "Layer 2", type = TrackType.SCENE))
            dao.insertTrack(TimelineTrack(projectId = id, name = "Layer 3", type = TrackType.SCENE))
            dao.insertTrack(TimelineTrack(projectId = id, name = "Layer 4", type = TrackType.SCENE))
            dao.insertTrack(TimelineTrack(projectId = id, name = "Layer 5", type = TrackType.SCENE))
            dao.insertTrack(TimelineTrack(projectId = id, name = "GIF Layer", type = TrackType.GIF))
            dao.insertTrack(TimelineTrack(projectId = id, name = "Audio", type = TrackType.AUDIO))
        }
    }

    fun addScene(projectId: Long, trackId: Long?, name: String) {
        viewModelScope.launch {
            val sequence = scenes.value.size
            val sceneId = dao.insertScene(Scene(projectId = projectId, name = name, sequence = sequence))
            // Create first frame
            dao.insertFrame(Frame(sceneId = sceneId, sequence = 0))

            // Add to timeline
            val targetTrack = tracks.value.find { it.id == trackId && it.type == TrackType.SCENE }
                ?: tracks.value.find { it.type == TrackType.SCENE }
            
            if (targetTrack != null) {
                val startTime = clips.value
                    .filter { it.trackId == targetTrack.id }
                    .maxOfOrNull { it.startTimeMs + it.durationMs } ?: 0L
                
                dao.insertClip(TimelineClip(
                    trackId = targetTrack.id,
                    startTimeMs = startTime,
                    durationMs = 2000L,
                    content = sceneId.toString()
                ))
            }
        }
    }

    fun addGif(trackId: Long, gifUri: String) {
        viewModelScope.launch {
            val track = tracks.value.find { it.id == trackId } ?: return@launch
            val startTime = clips.value
                .filter { it.trackId == trackId }
                .maxOfOrNull { it.startTimeMs + it.durationMs } ?: 0L

            dao.insertClip(TimelineClip(
                trackId = trackId,
                startTimeMs = startTime,
                durationMs = 2000L,
                content = gifUri
            ))
        }
    }

    private val _currentSceneId = MutableStateFlow<Long?>(null)
    val currentSceneId = _currentSceneId.asStateFlow()

    val currentFrames = _currentSceneId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else dao.getFramesByScene(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFrameIndex = MutableStateFlow(0)
    val selectedFrameIndex = _selectedFrameIndex.asStateFlow()

    fun getFrames(sceneId: Long): Flow<List<Frame>> = dao.getFramesByScene(sceneId)

    fun selectScene(id: Long) {
        _currentSceneId.value = id
        _selectedFrameIndex.value = 0
    }

    fun selectFrame(index: Int) {
        _selectedFrameIndex.value = index
    }

    fun addFrame() {
        val sceneId = _currentSceneId.value ?: return
        viewModelScope.launch {
            val sequence = dao.getFrameCount(sceneId)
            dao.insertFrame(Frame(sceneId = sceneId, sequence = sequence))
            _selectedFrameIndex.value = sequence
        }
    }

    fun saveFrame(strokesJson: String) {
        val frames = currentFrames.value
        val index = _selectedFrameIndex.value
        if (index < frames.size) {
            val frame = frames[index].copy(strokesJson = strokesJson)
            viewModelScope.launch {
                dao.updateFrame(frame)
            }
        }
    }

    fun updateClipDuration(clipId: Long, newDurationMs: Long) {
        viewModelScope.launch {
            val allClips = clips.value
            val targetClip = allClips.find { it.id == clipId } ?: return@launch
            
            val durationChange = newDurationMs - targetClip.durationMs
            if (durationChange == 0L) return@launch

            // Update target clip
            dao.updateClip(targetClip.copy(durationMs = newDurationMs))

            // Shift subsequent clips in the same track
            allClips.filter { it.trackId == targetClip.trackId && it.startTimeMs > targetClip.startTimeMs }
                .forEach { clip ->
                    dao.updateClip(clip.copy(startTimeMs = clip.startTimeMs + durationChange))
                }
        }
    }
}
