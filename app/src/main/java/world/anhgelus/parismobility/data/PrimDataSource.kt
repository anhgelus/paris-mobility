package world.anhgelus.parismobility.data

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
        val json = Json {
            ignoreUnknownKeys = true
        }
        val disruptions = json.decodeFromString<Disruptions>(content).disruptions
        val v = disruptions
            .filter { it.isHappening() && it.severity != Disruptions.Severity.INFORMATION }
            .fold(mutableMapOf<String, MutableList<Disruption>>()) { acc, disruption ->
                disruption.impactedSections?.forEach {
                    val id = it.id.split(":")[2]
                    val part = acc[id] ?: mutableListOf()
                    part.add(disruption)
                    acc[id] = part
                }
                acc
            }
        emit(v)
    }
}

fun parsePrismTime(time: String): LocalDateTime {
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
    @SerialName("applicationPeriods") val periods: List<Disruptions.Period>,
    val cause: String,
    val severity: Disruptions.Severity,
    val title: String,
    val message: String,
    val shortMessage: String? = null,
    @SerialName("impactedSections") val impactedSections: List<Disruptions.ImpactedSection>? = null,
) : Comparable<Disruption> {
    fun isHappening(): Boolean {
        val now = LocalDateTime.now()
        return periods.any {
            parsePrismTime(it.begin).isBefore(now) && parsePrismTime(it.end).isAfter(now)
        }
    }

    override fun compareTo(other: Disruption): Int {
        return severity.compareTo(other.severity)
    }
}

@Serializable
data class Disruptions(
    val disruptions: List<Disruption>
) {
    @Serializable
    data class Period(
        val begin: String,
        val end: String,
    )

    @Serializable
    data class ImpactedSection(
        @SerialName("lineId") val id: String,
    )

    @Serializable
    enum class Severity(
        val color: @Composable (() -> Color) = { Color.Transparent }
    ) {
        @SerialName("INFORMATION")
        INFORMATION,

        @SerialName("PERTURBEE")
        DISRUPT(({ CustomColorScheme.warning })),

        @SerialName("BLOQUANTE")
        BLOCKING(({ CustomColorScheme.error })),
    }
}