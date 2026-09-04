package world.anhgelus.parismobility.data.backend

import android.net.ConnectivityManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import world.anhgelus.parismobility.BuildConfig
import java.net.Socket
import java.net.SocketException
import javax.net.ssl.SSLSocketFactory

data class OneShotConnection(
	val conn: ConnectivityManager,
	private var socket: Socket? = null
) : Connection {
	private val _isConnected: MutableStateFlow<Boolean>
	override val isConnected
		get() = _isConnected.asStateFlow()

	init {
		conn.activeNetwork?.let {
			try {
				socket = SSLSocketFactory.getDefault().createSocket(
					BuildConfig.SERVER_HOSTNAME,
					BuildConfig.SERVER_PORT,
				)
				Log.i("OneShotConnection", "connected to the backend")
			} catch (e: Exception) {
				Log.w("OneShotConnection", "cannot connect to the backend: ${e.message}")
			}
		}
		_isConnected = MutableStateFlow(socket != null && socket?.isConnected ?: false)
	}

	private val mutex = Mutex()


	override suspend fun send(
		msg: Message,
		body: ByteArray
	): Pair<Message.Kind, ByteArray>? {
		if (!_isConnected.value) return null
		mutex.withLock {
			try {
				socket?.let { sock ->
					msg.encode(body).let { sock.getOutputStream()?.write(it) }
					return Message.decode(sock.getInputStream())
				}
			} catch (e: SocketException) {
				Log.w("BackendConnection", "connection lost: ${e.message}")
				close()
			}
			return null
		}
	}

	override fun close() {
		socket?.close()?.also { socket = null }
	}
}