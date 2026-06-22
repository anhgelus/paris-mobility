package world.anhgelus.parismobility.data

import android.annotation.SuppressLint
import android.content.res.Resources
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import world.anhgelus.parismobility.R

object LinesDataSource {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getLines(res: Resources): Map<Line.TransportMode, List<Line>> {
        return withContext(Dispatchers.IO) {
            val input = res
                .openRawResource(R.raw.lines)
                .bufferedReader()
                .use { it.readText() }
            json.decodeFromString<List<Line>>(input)
                .filter { it.status == Line.Status.ACTIVE }
                .fold(mutableMapOf<Line.TransportMode, MutableList<Line>>()) { acc, line ->
                    val lines = acc[line.mode] ?: mutableListOf()
                    line.loadResource(res)
                    lines.add(line)
                    acc[line.mode] = lines
                    acc
                }
        }
    }

    suspend fun getStops(res: Resources): Map<String, List<Stop>> {
        return withContext(Dispatchers.IO) {
            val input = res
                .openRawResource(R.raw.stops)
                .bufferedReader()
                .use { it.readText() }
            json.decodeFromString<List<Stop>>(input)
                .fold(mutableMapOf<String, MutableList<Stop>>()) { acc, stop ->
                    val lines = acc[stop.line] ?: mutableListOf()
                    lines.add(stop)
                    acc[stop.line] = lines
                    acc
                }
        }
    }
}

@Stable
@Serializable
data class Line(
    @SerialName("id_line") val id: String,
    @SerialName("name_line") val name: String,
    @SerialName("shortname_line") val shortName: String = name,
    @SerialName("transportmode") val mode: TransportMode,
    @SerialName("transportsubmode") val submode: String? = null,
    @SerialName("id_groupoflines") val groupOfLines: String? = null,
    @SerialName("networkname") val network: String? = null,
    @SerialName("status") val status: Status = Status.INACTIVE,
    @SerialName("colourweb_hexa") private val rawColor: String,
    @Transient val color: Color = Color(("#$rawColor").toColorInt()),
    @SerialName("textcolourweb_hexa") private val rawTextColor: String,
    @Transient val textColor: Color = Color(("#$rawTextColor").toColorInt()),
) : Comparable<Line> {
    enum class TransportMode(val hasSubMode: Boolean = false) {
        @SerialName("bus")
        BUS(true),

        @SerialName("rail")
        RAIL(true),

        @SerialName("funicular")
        FUNICULAR,

        @SerialName("metro")
        METRO,

        @SerialName("tram")
        TRAM,

        @SerialName("cableway")
        CABLEWAY,

        @SerialName("water")
        WATER
    }

    enum class Status {
        @SerialName("active")
        ACTIVE,

        @SerialName("prochainement active")
        INACTIVE
    }

    @Transient
    val disruptionSeverity = mutableStateOf(Severity.INFORMATION)

    @Transient
    private var resource: Int = 0

    fun setDisruption(sev: Severity?): Line {
        disruptionSeverity.value = sev ?: Severity.INFORMATION
        return this
    }

    // because we load it dynamically
    @SuppressLint("DiscouragedApi")
    fun loadResource(res: Resources): Boolean {
        val id = res.getIdentifier(
            "line_${id.lowercase()}",
            "drawable",
            "world.anhgelus.parismobility"
        )
        resource = id
        return id != 0
    }

    private val metroBisSuffix = "B"
    private val tramPrefix = "T"

    fun getResource() =
        if (resource != 0) resource
        else throw IllegalArgumentException("resource is 0 for $this")

    override fun compareTo(other: Line): Int {
        if (mode != other.mode) throw IllegalArgumentException("must have the same kind")
        return when (mode) {
            TransportMode.TRAM -> {
                var na = name.removePrefix(tramPrefix)
                var nb = other.name.removePrefix(tramPrefix)
                if (!na.last().isDigit()) na = na.removeSuffix(na.last().toString())
                if (!nb.last().isDigit()) nb = nb.removeSuffix(nb.last().toString())
                val ln = na.length - nb.length
                if (ln != 0) ln
                else String.CASE_INSENSITIVE_ORDER.compare(name, other.name)
            }

            TransportMode.RAIL -> String.CASE_INSENSITIVE_ORDER.compare(name, other.name)

            else -> {
                val ln = name.removeSuffix(metroBisSuffix).length - other.name.removeSuffix(
                    metroBisSuffix
                ).length
                if (ln != 0) ln
                else String.CASE_INSENSITIVE_ORDER.compare(name, other.name)
            }
        }
    }
}

@Stable
@Immutable
@Serializable
data class Stop(
    @SerialName("id_gares") val id: Int,
    @SerialName("idrefligc") val line: String,
    @SerialName("nom_iv") val name: String
)