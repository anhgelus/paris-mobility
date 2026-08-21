package world.anhgelus.parismobility.data

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import world.anhgelus.parismobility.BuildConfig
import java.nio.channels.UnresolvedAddressException
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

    suspend fun monitorStop(stop: Stop): Result<MonitorStop, NetworkError> {
        return get<Siri>("stop-monitoring?MonitoringRef=STIF%3AStopArea%3ASP%3A${stop.zda}%3A").map {
            it.root.delivery.monitorStop.first()
        }
    }
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