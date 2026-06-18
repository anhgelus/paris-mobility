package world.anhgelus.parismobility.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import world.anhgelus.parismobility.models.LineDataState
import world.anhgelus.parismobility.models.LineKind
import world.anhgelus.parismobility.models.LinesGroupDataState
import java.io.FileNotFoundException

class LocalDataRepository {
    val lines: List<LinesGroupDataState>

    private constructor(ctx: Context) {
        lines = listOf(
            newGroup(ctx, LineKind.RER),
            newGroup(ctx, LineKind.TRANSILIEN),
            newGroup(ctx, LineKind.METRO),
            newGroup(ctx, LineKind.TRAM),
        )
    }
//    val bus = newGroup("Bus", "", R.raw.bus)

    private fun newGroup(ctx: Context, kind: LineKind): LinesGroupDataState {
        val json = Json { ignoreUnknownKeys = true }
        val input = ctx.resources
            .openRawResource(kind.data)
            .bufferedReader()
            .use { it.readText() }
        val parsed = json.decodeFromString<LinesJson>(input)
        return LinesGroupDataState(kind, parsed.toState(ctx, kind))
    }

    companion object {
        private var INSTANCE: LocalDataRepository? = null

        fun get(ctx: Context): LocalDataRepository {
            if (INSTANCE == null) INSTANCE = LocalDataRepository(ctx)
            return INSTANCE!!
        }
    }
}

@Serializable
private data class LinesJson(val dataObjects: DataObjects) {
    // because we retrieve the logo dynamically
    @SuppressLint("DiscouragedApi")
    fun toState(ctx: Context, kind: LineKind): List<LineDataState> {
        val lines = dataObjects.compositeFrame.frames.generalFrame[1].members.lines!!
        return lines.filter {
            try {
                it.status == LineStatus.Active &&
                        if (kind != LineKind.RER) it.name.removePrefix("RER ") == it.name
                        else true
            } catch (e: Exception) {
                throw Exception("cannot filter ${it.name}", e)
            }
        }.map {
            it.members.lineRef.forEach { ref ->
                val id = ref.ref.split(":")[2]
                val res = ctx.resources.getIdentifier(
                    "${kind.prefix}_${id.lowercase()}",
                    "drawable",
                    "world.anhgelus.parismobility",
                )
                if (res == Resources.ID_NULL) return@forEach
                return@map LineDataState(it.name, id, res, kind)
            }
            throw FileNotFoundException("missing logo for ${it.name}")
        }.sorted()
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
        @SerialName("TransportMode") val kind: String,
        val members: LineMembers
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