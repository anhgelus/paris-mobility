package world.anhgelus.parismobility.data.backend

import android.net.ConnectivityManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import world.anhgelus.parismobility.BuildConfig
import java.net.Socket
import java.net.SocketException

class OneShotConnection(
	conn: ConnectivityManager,
) : Connection {
	private val _isConnected: MutableStateFlow<Boolean>
	override val isConnected
		get() = _isConnected.asStateFlow()
	private val socket: Socket?

	init {
		var sock: Socket? = null
		conn.activeNetwork?.let { network ->
			try {
				sock = network.socketFactory.createSocket(
					BuildConfig.SERVER_HOSTNAME,
					BuildConfig.SERVER_PORT,
				)
				Log.i("BackendConnection", "connected to the backend")
			} catch (e: Exception) {
				Log.w("BackendConnection", "cannot connect to the backend: ${e.message}")
			}
		}
		_isConnected = MutableStateFlow(sock != null && sock.isConnected)
		socket = sock
	}


	override suspend fun send(
		msg: Message,
		body: ByteArray
	): Pair<Message.Kind, ByteArray>? {
		try {
			if (socket == null) {
				return null
			}
			msg.encode(body).let { socket.getOutputStream()?.write(it) }
			return Message.decode(withContext(Dispatchers.IO) { socket.getInputStream() })
		} catch (e: SocketException) {
			Log.w("BackendConnection", "connection lost: ${e.message}")
			close()
			return null
		}
	}

	override fun close() {
		socket?.close()
	}
}