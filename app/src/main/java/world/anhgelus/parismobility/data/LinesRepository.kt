package world.anhgelus.parismobility.data

import android.content.res.Resources
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import world.anhgelus.parismobility.models.LineKind

class LinesRepository(
    private val linesSource: LinesDataSource,
    private val primSource: PrimDataSource
) {

    private val _lines = MutableStateFlow<LineGroups>(mutableMapOf())
    val lines = _lines.asStateFlow()

    private val _stops = MutableStateFlow<LineStops>(mutableMapOf())
    val stops = _stops.asStateFlow()

    val disruptions = primSource.disruptions.onEach { updateLines(it) }

    val primErrors = primSource.primError

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

    private fun updateLines(
        disruptions: LineDisruptions,
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

typealias LineDisruptions = Map<String, List<Disruption>>

typealias LineStops = Map<String, List<Stop>>

operator fun LineStops.get(saved: SavedStop): Pair<SavedLine, Stop>? {
    return this[saved.line.line]?.firstOrNull { it.line == saved.line.line }
        ?.let { Pair(saved.line, it) }
}