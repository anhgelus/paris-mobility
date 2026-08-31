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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class LinesRepository {
	private constructor(back: BackendDataSource) {
		backendSource = back
	}

	private val backendSource: BackendDataSource

	private val _lines = MutableStateFlow<LineGroups>(mutableMapOf())
	val lines = _lines.asStateFlow()

	val disruptions = flow {
		while (true) {
			backendSource.disruptions().onSuccess {
				updateLines(it)
				emit(it)
			}
			delay(1.minutes)
		}
	}.flowOn(Dispatchers.IO)

	fun loadLines() {
		_lines.update { Companion.loadLines() }
	}

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
				delay(45.seconds)
			}
		}
		while (true) {
			(previous?.let {
				emit(it.update())
				10.seconds
			} ?: 1.seconds).let { delay(it) }
		}
	}.flowOn(Dispatchers.IO)

	fun monitorStop(vararg s: Stop) {
		monitoredStops.addAll(s)
	}

	fun stopMonitoringStop(vararg s: Stop) {
		monitoredStops.removeAll(s.toSet())
	}

	private fun updateLines(
		disruptions: Disruptions,
	) {
		_lines.update { v ->
			v.mapValues { (_, lines) ->
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
	}

	companion object {
		private var instance: LinesRepository? = null

		fun getInstance(): LinesRepository? {
			return instance
		}

		fun getOrCreateInstance(back: BackendDataSource): LinesRepository {
			instance?.let { return it }
			instance = LinesRepository(back)
			instance!!.let {
				it.loadLines()
				return it
			}
		}

		fun loadLines(): MutableMap<LineKind, Map<String, LineState>> {
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