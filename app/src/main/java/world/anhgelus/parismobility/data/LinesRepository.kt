package world.anhgelus.parismobility.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import world.anhgelus.parismobility.data.backend.BackendDataSource
import world.anhgelus.parismobility.models.LineKind
import java.time.LocalTime
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class LinesRepository(var backendSource: BackendDataSource) {
	private val _lines = MutableStateFlow(loadLines())
	val lines = _lines.asStateFlow()

	private val _lastSync = MutableStateFlow<LocalTime>(LocalTime.now())
	val lastSync = _lastSync.asStateFlow()

	val disruptions = flow {
		while (true) {
			backendSource.disruptions().onSuccess {
				updateLines(it)
				_lastSync.value = LocalTime.now()
				emit(it)
			}
			delay(if (!backendSource.isLimited) 1.minutes else 5.minutes)
		}
	}.flowOn(Dispatchers.IO)

	private var monitoredStops = mutableSetOf<Stop>()

	val monitorStops = flow {
		var previous: MonitoringStops? = null
		CoroutineScope(Dispatchers.IO).launch {
			while (true) {
				if (monitoredStops.isEmpty()) {
					delay(500.milliseconds)
					continue
				}
				backendSource.monitorStops(monitoredStops).onSuccess {
					previous = MonitoringStops(map = it, synced = true)
				}
				delay(if (!backendSource.isLimited) 45.seconds else 3.minutes)
			}
		}
		while (true) {
			(previous?.let {
				if (it.synced) _lastSync.value = LocalTime.now()
				emit(it.update())
				10.seconds
			} ?: 1.seconds).let { delay(it) }
		}
	}.flowOn(Dispatchers.IO)

	fun monitorStop(vararg s: Stop) = monitoredStops.addAll(s)

	fun stopMonitoringStop(vararg s: Stop) = monitoredStops.removeAll(s.toSet())

	private fun updateLines(disruptions: Disruptions) =
		_lines.update { updateLines(it, disruptions) }

	companion object {
		fun updateLines(lines: LineGroups, disruptions: Disruptions): LineGroups {
			return lines.mapValues { (_, lines) ->
				lines.mapValues { (_, line) ->
					line.setDisruption(
						disruptions[line.line.id]
							?.filter { it.isHappening() }
							?.minOrNull()
							?.severity
					)
				}
			}
		}

		fun loadLines(): LineGroups {
			val lines = LinesDataSource.getLines().toSortedMap()
			val resp = mutableMapOf<LineKind, Map<String, LineState>>()
			lines.forEach { (mode, lines) ->
				when (mode) {
					Line.TransportMode.BUS -> mapOf(LineKind.BUS to lines)
					Line.TransportMode.TRAM -> mapOf(LineKind.TRAM to lines)
					Line.TransportMode.METRO -> mapOf(LineKind.METRO to lines)
					Line.TransportMode.RAIL -> {
						mapOf(
							LineKind.RER to lines.filter { it.line.submode == "local" },
							LineKind.TRANSILIEN to lines.filter {
								// because the API doesn't contain the data of line V
								it.line.submode == "suburbanRailway" && it.line.name != "V"
							},
						)
					}

					else -> return@forEach // do nothing with unsupported modes
				}.let { map ->
					resp.putAll(map.mapValues { (_, lines) ->
						lines.sorted().fold(mutableMapOf()) { acc, line ->
							acc.also { acc[line.line.id] = line }
						}
					})
				}
			}
			return resp
		}
	}
}

typealias LineGroups = Map<LineKind, Map<String, LineState>>

operator fun LineGroups.get(saved: SavedLine): Pair<LineKind, LineState>? {
	return this[saved.kind]?.get(saved.line)?.let { Pair(saved.kind, it) }
}

typealias LineStops = Map<String, List<Stop>>

operator fun LineStops.get(saved: SavedStop): Pair<SavedLine, Stop>? {
	return this[saved.line.line]?.firstOrNull { it.id == saved.stop }
		?.let { Pair(saved.line, it) }
}