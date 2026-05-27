package `fun`.cybercode.simplyvisuals.uniblox_animate.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "scenes",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class Scene(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val sequence: Int
)

@Entity(
    tableName = "frames",
    foreignKeys = [
        ForeignKey(
            entity = Scene::class,
            parentColumns = ["id"],
            childColumns = ["sceneId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sceneId")]
)
data class Frame(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sceneId: Long,
    val sequence: Int,
    val strokesJson: String = "[]"
)

@Serializable
data class Stroke(
    val points: List<Point>,
    val color: Int,
    val width: Float
)

@Serializable
data class Point(val x: Float, val y: Float)

@Entity(
    tableName = "timeline_tracks",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class TimelineTrack(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val type: TrackType
)

enum class TrackType { SCENE, AUDIO, GIF }

@Entity(
    tableName = "timeline_clips",
    foreignKeys = [
        ForeignKey(
            entity = TimelineTrack::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("trackId")]
)
data class TimelineClip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val startTimeMs: Long,
    val durationMs: Long,
    val content: String // sceneId or audioUri
)

@Dao
interface StudioDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Insert
    suspend fun insertProject(project: Project): Long

    @Query("SELECT * FROM scenes WHERE projectId = :projectId ORDER BY sequence ASC")
    fun getScenesByProject(projectId: Long): Flow<List<Scene>>

    @Insert
    suspend fun insertScene(scene: Scene): Long

    @Query("SELECT * FROM frames WHERE sceneId = :sceneId ORDER BY sequence ASC")
    fun getFramesByScene(sceneId: Long): Flow<List<Frame>>

    @Insert
    suspend fun insertFrame(frame: Frame): Long

    @Update
    suspend fun updateFrame(frame: Frame)

    @Query("SELECT * FROM timeline_tracks WHERE projectId = :projectId")
    fun getTracksByProject(projectId: Long): Flow<List<TimelineTrack>>

    @Insert
    suspend fun insertTrack(track: TimelineTrack): Long

    @Query("SELECT * FROM timeline_clips WHERE trackId = :trackId")
    fun getClipsByTrack(trackId: Long): Flow<List<TimelineClip>>

    @Insert
    suspend fun insertClip(clip: TimelineClip): Long

    @Update
    suspend fun updateClip(clip: TimelineClip)
}

@Database(
    entities = [Project::class, Scene::class, Frame::class, TimelineTrack::class, TimelineClip::class],
    version = 1,
    exportSchema = false
)
abstract class StudioDatabase : RoomDatabase() {
    abstract fun studioDao(): StudioDao
}
