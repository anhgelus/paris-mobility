package world.anhgelus.parismobility.widget

import android.content.Context
import android.net.ConnectivityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import world.anhgelus.parismobility.data.LinesRepository
import world.anhgelus.parismobility.data.MonitoringMap
import world.anhgelus.parismobility.data.STOPS
import world.anhgelus.parismobility.data.SavedStop
import world.anhgelus.parismobility.data.backend.BackendDataSource
import world.anhgelus.parismobility.data.backend.OneShotConnection
import java.time.LocalTime

class WidgetRepository(private val savedStops: Collection<SavedStop>) {
	private val _lastSync = MutableStateFlow(LocalTime.now())
	val lastSync = _lastSync.asStateFlow()

	private val _isConnected = MutableStateFlow(false)
	val isConnected = _isConnected.asStateFlow()

	private val _lines = MutableStateFlow(LinesRepository.loadLines())
	val lines = _lines.asStateFlow()

	private val _stops = MutableStateFlow<MonitoringMap>(emptyMap())
	val stops = _stops.asStateFlow()

	suspend fun update(ctx: Context) {
		OneShotConnection(
			ctx.getSystemService(ConnectivityManager::class.java)!!
		).use { update(BackendDataSource(it)) }
	}

	suspend fun update(conn: BackendDataSource?) {
		if (conn == null) {
			_isConnected.update { false }
			return
		}
		conn.disruptions().onSuccess { dis ->
			_lines.update { LinesRepository.updateLines(it, dis) }
		}.onFailure {
			_isConnected.update { false }
			conn.close()
			return
		}
		conn.monitorStops(savedStops.map { STOPS[it.line.line]!![it.stop]!! })
			.onSuccess { s ->
				_stops.update { s }
			}.onFailure {
				_isConnected.update { false }
				conn.close()
				return
			}
		_isConnected.update { true }
		_lastSync.update { LocalTime.now() }
	}
}