package world.anhgelus.parismobility.data

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.http.appendEncodedPathSegments
import io.ktor.http.userAgent
import kotlinx.coroutines.flow.flow
import kotlinx.io.IOException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import world.anhgelus.parismobility.BuildConfig
import world.anhgelus.parismobility.ui.theme.CustomColorScheme
import java.time.LocalDateTime

object PrimDataSource {
    private suspend fun get(path: String): HttpResponse {
        val client = HttpClient {}

        return client.request {
            method = HttpMethod.Get
            url {
                protocol = URLProtocol.HTTPS
                host = "prim.iledefrance-mobilites.fr"
                appendEncodedPathSegments("marketplace", path)
            }
            userAgent("Paris Mobilité/1.0")
            headers {
                append("Accept", "application/json")
                append("apiKey", BuildConfig.PRIM_TOKEN)
            }
        }.let {
            if (it.status.value >= 400) throw IOException("invalid response: ${it.bodyAsText()}")
            else it
        }
    }

    val disruptions = flow {
        val resp = get("disruptions_bulk/disruptions/v2")
        val content = resp.bodyAsText()
        val json = Json { ignoreUnknownKeys = true }
        val disruptions = json.decodeFromString<Disruptions>(content)
        val alreadyAdded = mutableSetOf<String>()
        val indexedDisruptions = disruptions.disruptions
            .filter { !it.periods.isEmpty() && alreadyAdded.add(it.title) }
            .fold(mutableMapOf<String, Disruption>()) { acc, disruption ->
                acc[disruption.id] = disruption
                acc
            }
        val linesDisruptions = disruptions.lines
            .fold(mutableMapOf<String, List<Disruption>>()) { acc, l ->
                val id = l.id.split(":")[2]
                val d = mutableListOf<Disruption>()
                l.impactedObjects.firstOrNull { it.type == "line" }?.disruptionIds?.forEach {
                    indexedDisruptions[it]?.let { v -> d.add(v) }
                }
                if (!d.isEmpty()) acc[id] = d.sorted()
                acc
            }
        emit(linesDisruptions)
    }
}

fun parsePrimTime(time: String): LocalDateTime {
    val year = time.substring(0, 4).toInt()
    val month = time.substring(4, 6).toInt()
    val day = time.substring(6, 8).toInt()
    // ignoring the T
    val hour = time.substring(9, 11).toInt()
    val minute = time.substring(11, 13).toInt()
    val second = time.substring(13, 15).toInt()
    return LocalDateTime.of(year, month, day, hour, minute, second)
}

@Serializable
data class Disruption(
    val id: String,
    @SerialName("applicationPeriods") private val stringPeriods: List<Disruptions.StringPeriod>,
    val cause: String,
    val severity: Severity,
    val title: String,
    val message: String,
    val shortMessage: String? = null,
    @SerialName("impactedSections") val impactedSections: List<Disruptions.ImpactedSection>? = null,
) : Comparable<Disruption> {
    @Transient
    val periods: List<Period> = stringPeriods.map { it.toPeriod() }.sorted()
        get() = field.filter { it.end.isAfter(LocalDateTime.now()) }

    fun isHappening(): Boolean {
        val now = LocalDateTime.now()
        return periods.any { it.begin <= now }
    }

    fun currentOrNextPeriod(): Period {
        return periods.min()
    }

    override fun compareTo(other: Disruption): Int {
        val res = currentOrNextPeriod().compareTo(other.currentOrNextPeriod())
        if (res != 0) return res
        return -severity.compareTo(other.severity)
    }
}

data class Period(
    val begin: LocalDateTime,
    val end: LocalDateTime
) : Comparable<Period> {
    override fun compareTo(other: Period): Int {
        return begin.compareTo(other.begin)
    }
}

@Serializable
enum class Severity {
    @SerialName("INFORMATION")
    INFORMATION,

    @SerialName("PERTURBEE")
    DISRUPT,

    @SerialName("BLOQUANTE")
    BLOCKING;

    val color: Pair<Color, Color>
        @Composable
        get() = when (this) {
            INFORMATION -> Pair(Color.Transparent, MaterialTheme.colorScheme.onSurface)
            DISRUPT -> Pair(CustomColorScheme.warning, CustomColorScheme.onWarning)
            BLOCKING -> Pair(CustomColorScheme.error, CustomColorScheme.onError)
        }
}

@Serializable
data class Disruptions(
    val disruptions: List<Disruption>,
    val lines: List<LineDisruptions>
) {
    @Serializable
    data class StringPeriod(
        val begin: String,
        val end: String,
    ) {
        fun toPeriod(): Period {
            return Period(parsePrimTime(begin), parsePrimTime(end))
        }
    }

    @Serializable
    data class ImpactedSection(
        @SerialName("lineId") val id: String,
    )
}

@Serializable
data class LineDisruptions(
    val id: String,
    val name: String,
    val mode: String,
    val impactedObjects: List<ImpactedObject>
) {
    @Serializable
    data class ImpactedObject(
        val type: String,
        val id: String,
        val name: String,
        val disruptionIds: List<String>
    )
}