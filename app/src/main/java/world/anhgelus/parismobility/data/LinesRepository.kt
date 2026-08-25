package world.anhgelus.parismobility.data

import android.content.res.Resources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import world.anhgelus.parismobility.data.backend.BackendDataSource
import world.anhgelus.parismobility.models.LineKind
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class LinesRepository(
    private val linesSource: LinesDataSource,
    private val backendSource: BackendDataSource,
) {

    private val _lines = MutableStateFlow<LineGroups>(mutableMapOf())
    val lines = _lines.asStateFlow()

    private val _stops = MutableStateFlow<LineStops>(mutableMapOf())
    val stops = _stops.asStateFlow()

    val disruptions = flow {
        while (true) {
            val res = backendSource.disruptions()
            res.onSuccess { updateLines(it) }
            emit(res)
            delay(1.minutes)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun loadLines(res: Resources) {
        val lines = linesSource.getLines(res)
        val resp = mutableMapOf<LineKind, List<Line>>()
        lines.forEach { (mode, lines) ->
            when (mode) {
                Line.TransportMode.BUS -> resp[LineKind.BUS] = lines.sorted()
                Line.TransportMode.TRAM -> resp[LineKind.TRAM] = lines.sorted()
                Line.TransportMode.METRO -> resp[LineKind.METRO] = lines.sorted()
                Line.TransportMode.RAIL -> {
                    resp[LineKind.RER] = lines.filter { it.submode == "local" }.sorted()
                    resp[LineKind.TRANSILIEN] = lines.filter {
                        // because the API doesn't contain the data of line V
                        it.submode == "suburbanRailway" && it.name != "V"
                    }.sorted()
                }

                else -> {} // do nothing with unsupported modes
            }
        }
        _lines.update { resp }
    }

    suspend fun loadStops(res: Resources) {
        _stops.update { linesSource.getStops(res) }
    }

    private var monitoredStops = mutableSetOf<Stop>()
    private var lastUpdateStops: ZonedDateTime? = null

    val monitorStops = flow {
        while (true) {
            if (
                monitoredStops.isEmpty() || lastUpdateStops?.let {
                    ChronoUnit.SECONDS.between(it, ZonedDateTime.now()) < 45
                } ?: false
            ) {
                delay(500.milliseconds)
                continue
            }
            val res = backendSource.monitorStops(monitoredStops)
            emit(res)
            lastUpdateStops = ZonedDateTime.now()
        }
    }.flowOn(Dispatchers.IO)
        .shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.Lazily)

    fun monitorStop(vararg s: Stop) {
        monitoredStops.addAll(s)
        lastUpdateStops = null
    }

    fun stopMonitoringStop(vararg s: Stop) {
        monitoredStops.removeAll(s.toSet())
        lastUpdateStops = null
    }

    private fun updateLines(
        disruptions: Disruptions,
    ) {
        _lines.update { v ->
            v.mapValues { (_, lines) ->
                lines.map { line ->
                    line.setDisruption(
                        disruptions[line.id]
                            ?.filter { it.isHappening() }
                            ?.minOrNull()
                            ?.severity
                    )
                }
            }
        }
    }
}

typealias LineGroups = Map<LineKind, List<Line>>

operator fun LineGroups.get(saved: SavedLine): Pair<LineKind, Line>? {
    return this[saved.kind]?.firstOrNull { it.id == saved.line }?.let { Pair(saved.kind, it) }
}

typealias LineStops = Map<String, List<Stop>>

operator fun LineStops.get(saved: SavedStop): Pair<SavedLine, Stop>? {
    return this[saved.line.line]?.firstOrNull { it.id == saved.stop }
        ?.let { Pair(saved.line, it) }
}