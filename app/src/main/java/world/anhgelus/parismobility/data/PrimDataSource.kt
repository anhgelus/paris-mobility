package world.anhgelus.parismobility.data

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.encoding.zstd.ZstdEncoder
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.http.appendEncodedPathSegments
import io.ktor.http.userAgent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import world.anhgelus.parismobility.BuildConfig
import world.anhgelus.parismobility.ui.theme.CustomColorScheme
import java.nio.channels.UnresolvedAddressException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object PrimDataSource {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(ContentEncoding) {
            customEncoder(ZstdEncoder())
            gzip()
        }
    }

    private suspend inline fun <reified T> get(path: String): Result<T, NetworkError> {
        val resp = try {
            httpClient.request {
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
            }
        } catch (_: UnresolvedAddressException) {
            return Result.Error(NetworkError.NO_INTERNET)
        }
        return when (resp.status.value) {
            in 200..299 -> {
                try {
                    val data = resp.body<T>()
                    Result.Ok(data)
                } catch (_: SerializationException) {
                    Result.Error(NetworkError.INVALID_DATA)
                }
            }

            400 -> Result.Error(NetworkError.INVALID_DATA)
            401 -> Result.Error(NetworkError.INVALID_AUTH)
            429 -> Result.Error(NetworkError.RATE_LIMITED)
            in 500..599 -> Result.Error(NetworkError.SERVER_ERROR)
            else -> Result.Error(NetworkError.UNKNOWN_ERROR)
        }
    }

    val primError = mutableStateOf<NetworkError?>(null)

    val disruptions = flow {
        var disruptions: Disruptions? = null
        get<Disruptions>("disruptions_bulk/disruptions/v2").onSuccess {
            disruptions = it
        }.onError {
            primError.value = it
        }
        if (disruptions == null) return@flow
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

    suspend fun monitorStop(stop: Stop): Result<MonitorStop, NetworkError> {
        return get<Siri>("stop-monitoring?MonitoringRef=STIF%3AStopArea%3ASP%3A${stop.zda}%3A").map {
            it.root.delivery.monitorStop.first()
        }
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

@Stable
@Serializable
data class Disruption(
    val id: String,
    @SerialName("applicationPeriods") private val stringPeriods: List<Disruptions.StringPeriod>? = null,
    val cause: String,
    val severity: Severity,
    val title: String,
    val message: String,
    val shortMessage: String? = null,
    @SerialName("impactedSections") val impactedSections: List<Disruptions.ImpactedSection>? = null,
) : Comparable<Disruption> {
    @Transient
    val periods: List<Period> = stringPeriods?.map { it.toPeriod() }?.sorted() ?: emptyList()
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

@Stable
@Immutable
data class Period(
    val begin: LocalDateTime,
    val end: LocalDateTime
) : Comparable<Period> {
    override fun compareTo(other: Period): Int {
        return begin.compareTo(other.begin)
    }
}

@Stable
@Immutable
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

@Stable
@Serializable
data class Disruptions(
    val disruptions: List<Disruption>,
    val lines: List<LineAffected>
) {
    @Stable
    @Immutable
    @Serializable
    data class StringPeriod(
        val begin: String = "19990102T010203", // skip data without "begin" set
        val end: String = begin,
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

@Stable
@Immutable
@Serializable
data class LineAffected(
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

@Stable
@Immutable
@Serializable
private data class Siri(
    @SerialName("Siri") val root: ServiceDelivery,
) {
    @Stable
    @Immutable
    @Serializable
    data class ServiceDelivery(
        @SerialName("ServiceDelivery") val delivery: StopMonitoringDelivery,
    )

    @Stable
    @Immutable
    @Serializable
    data class StopMonitoringDelivery(
        @SerialName("StopMonitoringDelivery") val monitorStop: List<MonitorStop>,
    )
}

@Stable
@Immutable
@Serializable
data class MonitorStop(
    // responseTimestamp is local time!
    @SerialName("ResponseTimestamp") val responseTimestamp: String,
    @SerialName("MonitoredStopVisit") val stopVisit: List<StopVisit>,
) {
    fun compact(
        stop: Stop,
        filter: (StopVisit) -> Boolean = { true }
    ): Map<String, MutableList<StopVisit>> {
        return stopVisit.filter {
            filter(it) && ChronoUnit.MINUTES.between(
                ZonedDateTime.now(),
                it.journey.monitored.zonedDateTime
            ) >= 0
        }
            .fold<StopVisit, MutableMap<String, MutableList<StopVisit>>>(mutableMapOf()) { acc, visit ->
                val k = visit.journey.monitored.destinationDisplay.first().value
                val v = acc[k] ?: mutableListOf()
                v.add(visit)
                acc[k] = v
                acc
            }.filterKeys { !stop.name.contains(it) && !it.contains(stop.name) }
    }

    @Stable
    @Immutable
    @Serializable
    data class StopVisit(
        @SerialName("RecordedAtTime") val recordedAt: String,
        @SerialName("MonitoringRef") val ref: Ref,
        @SerialName("MonitoredVehicleJourney") val journey: Journey,
    )

    @Stable
    @Immutable
    @Serializable
    data class Ref(
        @SerialName("value") val value: String
    )

    @Stable
    @Immutable
    @Serializable
    data class Journey(
        @SerialName("LineRef") val line: Ref,
        @SerialName("DirectionName") val directions: List<Ref>,
        @SerialName("JourneyNote") val notes: List<Ref>? = null,
        @SerialName("MonitoredCall") val monitored: Monitored,
        @SerialName("VehicleFeatureRef") val vehicleFeature: List<VehicleFeature> = emptyList(),
    )

    @Stable
    @Immutable
    @Serializable
    enum class VehicleFeature {
        @SerialName("shortTrain")
        ShortTrain,

        @SerialName("longTrain")
        LongTrain
    }

    @Stable
    @Immutable
    @Serializable
    data class Monitored(
        @SerialName("VehicleAtStop") val isStopped: Boolean,
        @SerialName("DestinationDisplay") val destinationDisplay: List<Ref>,
        @SerialName("ExpectedArrivalTime") private val arrivalTime: String? = null,
        @SerialName("ExpectedDepartureTime") private val departureTime: String? = null,
        @SerialName("DepartureStatus") val status: Status,
        @Transient val zonedDateTime: ZonedDateTime = ZonedDateTime.parse(
            arrivalTime ?: departureTime!!
        ).withZoneSameInstant(ZoneId.systemDefault())
    ) {
        init {
            if (arrivalTime == null && departureTime == null)
                throw IllegalArgumentException("cannot have arrival and departure time at null")
        }

        fun displayTime(): String {
            if (isStopped) return "À quai"
            val t = zonedDateTime
            val mins = ChronoUnit.MINUTES.between(ZonedDateTime.now(), t)
            if (mins == 0L) return "À l'approche"
            if (mins > 45) {
                val h = if (t.hour < 10) "0${t.hour}" else t.hour.toString()
                val m = if (t.minute < 10) "0${t.minute}" else t.minute.toString()
                return "$h:$m"
            }
            val hours = mins / 60
            val minString = (mins % 60).let { if (it < 10) "0$it" else "$it" }
            return if (hours != 0L) "$hours h $minString"
            else "${mins % 60} min"
        }
    }

    @Stable
    @Immutable
    @Serializable
    enum class Status {
        @SerialName("onTime")
        OnTime,

        @SerialName("early")
        Early,

        @SerialName("delayed")
        Delayed,

        @SerialName("cancelled")
        Cancelled,

        @SerialName("missed")
        Missed,

        @SerialName("arrived")
        Arrived,

        @SerialName("departed")
        Departed,

        @SerialName("notExpected")
        NotExpected,
    }
}