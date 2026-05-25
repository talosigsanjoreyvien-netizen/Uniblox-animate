package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        StudioDatabase::class.java, "studio-db"
    ).build()

    private val dao = db.studioDao()

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

    fun selectProject(id: Long) {
        _currentProjectId.value = id
    }

    fun createProject(name: String) {
        viewModelScope.launch {
            val id = dao.insertProject(Project(name = name))
            _currentProjectId.value = id
            // Add default tracks
            dao.insertTrack(TimelineTrack(projectId = id, name = "Animation", type = TrackType.SCENE))
            dao.insertTrack(TimelineTrack(projectId = id, name = "Audio", type = TrackType.AUDIO))
        }
    }

    fun addScene(projectId: Long, name: String) {
        viewModelScope.launch {
            val sequence = scenes.value.size
            val id = dao.insertScene(Scene(projectId = projectId, name = name, sequence = sequence))
            // Create first frame
            dao.insertFrame(Frame(sceneId = id, sequence = 0))
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
            val sequence = currentFrames.value.size
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
}
