package world.anhgelus.parismobility.data.backend

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import world.anhgelus.parismobility.BuildConfig
import java.net.Socket
import java.net.SocketException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class LongConnection(
	conn: ConnectivityManager,
) : ConnectivityManager.NetworkCallback(), Connection {
	init {
		val req = NetworkRequest.Builder()
			.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
			.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
			.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
			.addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
			.build()
		conn.registerNetworkCallback(req, this)

		CoroutineScope(Dispatchers.IO).launch {
			delay(1.seconds)
			while (true) {
				val req = sender.receive()
				try {
					socket()?.let { socket ->
						req.first.encode(req.second).let {
							socket.getOutputStream()?.write(it)
						}
						response.send(Message.decode(socket.getInputStream()))
						continue
					}
				} catch (e: SocketException) {
					Log.w("LongConnection", "connection lost: ${e.message}")
					previousSocket = null
					close()
				}
				response.send(null)
			}
		}
	}

	private val _isConnected = MutableStateFlow(true)
	override val isConnected = _isConnected.asStateFlow()

	private var network: Network? = null

	override fun onAvailable(network: Network) {
		super.onAvailable(network)
		waiting?.trySend(Unit)
		this.network = network
		waiting = null
	}

	override fun onLosing(network: Network, maxMsToLive: Int) {
		super.onLosing(network, maxMsToLive)
		this.network = null
		if (waiting == null) waiting = Channel()
		close()
	}

	override fun onLost(network: Network) {
		super.onLost(network)
		this.network = null
		if (waiting == null) waiting = Channel()
	}

	private suspend fun connect(timeout: Duration = 1.minutes): Socket? {
		close()
		return withTimeoutOrNull(timeout) {
			var dur = 2
			var socket: Socket? = null
			while (socket == null && isActive) {
				network?.let { network ->
					try {
						socket = network.socketFactory.createSocket(
							BuildConfig.SERVER_HOSTNAME,
							BuildConfig.SERVER_PORT,
						)
						Log.i("LongConnection", "connected to the backend")
						_isConnected.value = true
						break
					} catch (e: Exception) {
						Log.w(
							"LongConnection",
							"cannot connect to the backend: ${e.message}, retrying in $dur seconds"
						)
					}
				}
				_isConnected.value = false
				delay(dur.seconds)
				dur = dur.shl(1)
			}
			socket
		}
	}

	override fun close() {
		previousSocket?.let {
			_isConnected.value = false
			it.close()
		}
		previousSocket = null
	}

	private var previousSocket: Socket? = null

	private suspend fun socket(): Socket? {
		waiting?.receive()
		var res: Socket? = null
		previousSocket?.let {
			res = withTimeoutOrNull(10.seconds) {
				if (it.getInputStream().read(ByteArray(0)) == -1) null
				else it
			}
		}
		if (res != null) return res
		connect(10.minutes).also {
			previousSocket = it
			return it
		}
	}

	private var waiting: Channel<Unit>? = null

	private val sender: Channel<Pair<Message, ByteArray>> = Channel()
	private val response: Channel<Pair<Message.Kind, ByteArray>?> = Channel()

	override suspend fun send(msg: Message, body: ByteArray): Pair<Message.Kind, ByteArray>? {
		sender.send(Pair(msg, body))
		return response.receive()
	}
}