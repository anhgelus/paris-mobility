package world.anhgelus.parismobility.data

import android.content.res.Resources
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import world.anhgelus.parismobility.models.LineKind

class LinesRepository(
    private val linesSource: LinesDataSource,
    private val prismSource: PrimDataSource
) {

    private val _lines = MutableStateFlow<Map<LineKind, List<Line>>>(mutableMapOf())
    val lines = _lines.asStateFlow()

    private val _disruptions = MutableStateFlow<Map<String, List<Disruption>>>(mapOf())
    val disruptions = _disruptions.asStateFlow()

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
        updateLines(res)
    }

    suspend fun updateDisruptions() {
        prismSource.disruptions.collectLatest { dis ->
            _disruptions.update { dis }
            updateLines()
        }
    }

    private fun updateLines(groups: Map<LineKind, List<Line>> = _lines.value) {
        _lines.update {
            groups.mapValues { (_, lines) ->
                lines.map { line ->
                    line.setDisruption(
                        disruptions.value[line.id]
                            ?.filter { it.isHappening() }
                            ?.minOrNull()
                            ?.severity)
                }
            }
        }
    }
}