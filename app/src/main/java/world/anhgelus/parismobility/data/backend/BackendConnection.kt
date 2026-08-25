package world.anhgelus.parismobility.data.backend

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.ExperimentalSerializationApi
import world.anhgelus.parismobility.BuildConfig
import java.io.InputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream
import kotlin.experimental.or
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class BackendConnection(
    conn: ConnectivityManager,
) : ConnectivityManager.NetworkCallback() {
    init {
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()
        conn.registerNetworkCallback(req, this)

        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                waiting?.receive()
                previousSocket?.let {
                    _socket.send(it)
                    continue
                }
                connect().also {
                    _socket.send(it)
                    previousSocket = it
                }
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                val req = sender.receive()
                socket.receive()?.let { socket ->
                    req.first.encode(req.second).let {
                        socket.getOutputStream()?.write(it)
                    }
                    Message.decode(socket.getInputStream()).let {
                        response.send(it)
                    }
                }
            }
        }
    }

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
                        Log.i("BackendData", "connected to the backend")
                        break
                    } catch (e: Exception) {
                        Log.w(
                            "BackendData",
                            "cannot connect to the backend: ${e.message}, retrying in $dur seconds"
                        )
                    }
                }
                delay(dur.seconds)
                dur = dur.shl(1)
            }
            socket
        }
    }

    fun close() {
        previousSocket?.close()
        previousSocket = null
        _socket.trySend(null)
    }

    private var waiting: Channel<Unit>? = null
    private var previousSocket: Socket? = null

    private val _socket: Channel<Socket?> = Channel()
    val socket: ReceiveChannel<Socket?> = _socket

    private val sender: Channel<Pair<Message, ByteArray>> = Channel()
    private val response: Channel<Pair<Message.Kind, ByteArray>?> = Channel()

    suspend fun send(msg: Message, body: ByteArray): Pair<Message.Kind, ByteArray>? {
        sender.send(Pair(msg, body))
        return response.receive()
    }
}

data class Message(
    val kind: Kind,
    val flags: List<Flag>,
) {
    @OptIn(ExperimentalSerializationApi::class)
    fun encode(body: ByteArray): ByteArray {
        val ls = mutableListOf<Byte>()
        ls.add(kind.ordinal.toByte())
        flags.fold(0.toByte()) { acc, it ->
            acc.or(1.shl(it.ordinal).toByte())
        }.let { ls.add(it) }
        ByteBuffer.allocate(4)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(body.size)
            .array()
            .let { ls.addAll(it.toList()) }
        ls.add('\r'.code.toByte())
        ls.add('\n'.code.toByte())
        ls.addAll(body.toList())
        return ls.toByteArray()
    }

    enum class Kind {
        RESPONSE,
        INVALID_REQUEST,
        INTERNAL_ERROR,
        DISRUPTIONS,
        MONITORING,
        GOODBYE,
    }

    companion object {
        fun decode(input: InputStream): Pair<Kind, ByteArray>? {
            var buf = ByteArray(8)
            var n = input.read(buf)
            if (n == -1) return null
            if (n != buf.size) throw IllegalArgumentException("invalid message")
            val rawKind = buf[0]
            if (rawKind >= Kind.entries.size) throw IllegalArgumentException("unknown kind")
            val kind = Kind.entries[buf[0].toInt()]
            val rawFlags = buf[1]
            val flags = Flag.entries.fold(mutableListOf<Flag>()) { acc, it ->
                if (1.shl(it.ordinal).and(rawFlags.toInt()) != 0)
                    acc.add(it).let { acc }
                else acc
            }
            val len = ByteBuffer.wrap(buf, 2, 4).getInt()
            val b = mutableListOf<List<Byte>>()
            n = 0
            while (n < len) {
                val sub = ByteArray(1024)
                val nn = input.read(sub)
                if (nn < 0) throw IllegalArgumentException("invalid message")
                b.add(sub.slice(0..<nn))
                n += nn
            }
            buf = b.flatten().toByteArray()
            if (flags.contains(Flag.GZIP)) {
                buf.inputStream().use { input ->
                    GZIPInputStream(input).use { buf = it.readBytes() }
                }
            }
            return Pair(kind, buf)
        }
    }
}

enum class Flag {
    GZIP,
}