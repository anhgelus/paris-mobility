package world.anhgelus.parismobility.data

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import world.anhgelus.parismobility.R
import world.anhgelus.parismobility.models.LineDataState
import world.anhgelus.parismobility.models.LinesGroupDataState

class LocalDataRepository(private val ctx: Context) {
    val lines = listOf(
        newGroup("RER", R.raw.rer),
        newGroup("Transilien", R.raw.transilien),
        newGroup("Métro", R.raw.metro),
        newGroup("Tram", R.raw.tram),
    )
    val bus = newGroup("Bus", R.raw.bus)

    private fun newGroup(
        name: String,
        id: Int,
    ): LinesGroupDataState {
        val json = Json { ignoreUnknownKeys = true }
        val input = ctx.resources.openRawResource(id).bufferedReader().use { it.readText() }
        val parsed = json.decodeFromString<LinesJson>(input)
        return LinesGroupDataState(name, parsed.toState())
    }
}

@Serializable
private data class LinesJson(val dataObjects: DataObjects) {
    fun toState(): List<LineDataState> {
        val lines = dataObjects.compositeFrame.frames.generalFrame[1].members.lines!!
        return lines.filter { it.status == LineStatus.Active }.map {
            LineDataState(it.name, R.drawable.ic_launcher_foreground)
        }.sortedBy {
            it.name
        }
    }

    @Serializable
    data class DataObjects(@SerialName("CompositeFrame") val compositeFrame: CompositeFrame)

    @Serializable
    data class CompositeFrame(val frames: Frames)

    @Serializable
    data class Frames(@SerialName("GeneralFrame") val generalFrame: List<GeneralFrame>)

    @Serializable
    data class GeneralFrame(val members: Members)

    @Serializable
    data class Members(
        @SerialName("SchematicMap") val schematicMaps: List<SchematicMap>? = null,
        @SerialName("GroupOfLines") val lines: List<GroupOfLines>? = null,
    )

    @Serializable
    data class SchematicMap(
        @SerialName("Name") val name: String,
        @SerialName("ImageUri") val imageUri: String,
        @SerialName("DepictedObjectRef") val objectRef: DepictedObjectRef
    )

    @Serializable
    data class DepictedObjectRef(
        val version: String,
        val ref: String,
    )

    @Serializable
    data class GroupOfLines(
        val id: String,
        val status: LineStatus,
        @SerialName("Name") val name: String,
        @SerialName("TransportMode") val kind: String
    )

    @Serializable
    enum class LineStatus {
        @SerialName("active")
        Active,

        @SerialName("inactive")
        Inactive
    }

    @Serializable
    data class LineMembers(@SerialName("LineRef") val lineRef: List<LineRef>)

    @Serializable
    data class LineRef(val ref: String)
}