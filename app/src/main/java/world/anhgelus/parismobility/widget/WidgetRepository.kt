package world.anhgelus.parismobility.widget

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import world.anhgelus.parismobility.data.LinesRepository
import world.anhgelus.parismobility.data.MonitoringMap
import world.anhgelus.parismobility.data.STOPS
import world.anhgelus.parismobility.data.SavedStop
import world.anhgelus.parismobility.data.backend.BackendDataSource
import java.time.LocalTime

class WidgetRepository(conn: BackendDataSource?, stops: Collection<SavedStop>) {
	private val _lastSync = MutableStateFlow(LocalTime.now())
	val lastSync = _lastSync.asStateFlow()

	private val _isConnected = MutableStateFlow(false)
	val isConnected = _isConnected.asStateFlow()

	private val _lines = MutableStateFlow(LinesRepository.loadLines())
	val lines = _lines.asStateFlow()

	private val _stops = MutableStateFlow<MonitoringMap>(emptyMap())
	val stops = _stops.asStateFlow()

	init {
		if (conn == null) {
			_isConnected.update { false }
		} else {
			CoroutineScope(Dispatchers.IO).launch {
				conn.disruptions().onSuccess { dis ->
					_lines.update { LinesRepository.updateLines(it, dis) }
				}.onFailure {
					_isConnected.update { false }
					conn.close()
					return@launch
				}
				conn.monitorStops(stops.map { STOPS[it.line.line]!![it.stop]!! })
					.onSuccess { stops ->
						_stops.update { stops }
					}.onFailure {
						_isConnected.update { false }
						conn.close()
						return@launch
					}
				_isConnected.update { true }
				_lastSync.update { LocalTime.now() }
				conn.close()
			}
		}
	}
}