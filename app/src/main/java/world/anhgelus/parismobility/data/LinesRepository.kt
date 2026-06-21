package world.anhgelus.parismobility.data

import android.content.res.Resources
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import world.anhgelus.parismobility.models.LineKind

class LinesRepository(
    private val linesSource: LinesDataSource,
    private val primSource: PrimDataSource
) {

    private val _lines = MutableStateFlow<LineGroups>(mutableMapOf())
    val lines = _lines.asStateFlow()

    val disruptions = primSource.disruptions.onEach { updateLines(it) }

    suspend fun initLines(res: Resources) {
        val lines = linesSource.getLines(res)
        val res = mutableMapOf<LineKind, List<Line>>()
        lines.forEach { (mode, lines) ->
            when (mode) {
                Line.TransportMode.BUS -> res[LineKind.BUS] = lines.sorted()
                Line.TransportMode.TRAM -> res[LineKind.TRAM] = lines.sorted()
                Line.TransportMode.METRO -> res[LineKind.METRO] = lines.sorted()
                Line.TransportMode.RAIL -> {
                    res[LineKind.RER] = lines.filter { it.submode == "local" }.sorted()
                    res[LineKind.TRANSILIEN] = lines.filter {
                        // because the API doesn't contain the data of line V
                        it.submode == "suburbanRailway" && it.name != "V"
                    }.sorted()
                }

                else -> {} // do nothing with unsupported modes
            }
        }
        _lines.update { res }
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
            }.let { Json.encodeToString(it) }.let { Json.decodeFromString(it) }
        }
    }
}

typealias LineGroups = Map<LineKind, List<Line>>

operator fun LineGroups.get(saved: SavedLine): Pair<LineKind, Line>? {
    return this[saved.kind]?.firstOrNull { it.id == saved.line }?.let { Pair(saved.kind, it) }
}

typealias LineDisruptions = Map<String, List<Disruption>>