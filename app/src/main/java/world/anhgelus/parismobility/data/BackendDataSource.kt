package world.anhgelus.parismobility.data

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import world.anhgelus.parismobility.BuildConfig
import java.io.InputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream
import kotlin.experimental.or

class BackendDataSource(
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
    }

    private var socket: Socket? = null

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        socket = network.socketFactory.createSocket(
            BuildConfig.SERVER_HOSTNAME,
            BuildConfig.SERVER_PORT,
        )
    }

    override fun onLosing(network: Network, maxMsToLive: Int) {
        super.onLosing(network, maxMsToLive)
        close()
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        socket = null
    }

    @OptIn(ExperimentalSerializationApi::class)
    private inline fun <reified R, reified T> get(kind: Kind, req: R): Result<T, NetworkError> {
        if (socket == null || !socket!!.isConnected) {
            return Result.Error(NetworkError.NO_INTERNET)
        }
        val sock = socket!!
        val req = Cbor.encodeToByteArray(req).toList()
        val msg = MessageHeader(kind, emptyList(), req.size.toUInt()).encode()
        msg.addAll(req)
        sock.getOutputStream().write(msg.toByteArray())
        val res = MessageHeader.decode(sock.getInputStream())
        return when (res.first) {
            Kind.RESPONSE -> Result.Ok(Cbor.decodeFromByteArray(res.second))
            Kind.INVALID_REQUEST -> Result.Error(
                NetworkError.UNKNOWN_ERROR,
                Cbor.decodeFromByteArray<ErrorResponse>(res.second).let {
                    it.error?.let { m -> Log.w("BackendData", m) }
                    it.message
                }
            )

            Kind.INTERNAL_ERROR, Kind.DISRUPTIONS, Kind.MONITORING -> Result.Error(NetworkError.SERVER_ERROR)
            Kind.GOODBYE -> throw IllegalArgumentException("Server disconnected")
        }
    }

    val disruptions = flow {
        emit(
            get<DisruptionsRequest, Disruptions>(
                Kind.DISRUPTIONS,
                DisruptionsRequest(emptyList(), emptyList())
            ),
        )
    }.flowOn(Dispatchers.IO)

    fun monitorStops(stops: Collection<Stop>): Flow<Result<MonitoringStops, NetworkError>> {
        return flow {
            emit(
                get<MonitoringRequest, MonitoringStops>(
                    Kind.MONITORING,
                    MonitoringRequest(stops.map { it.zda.toString() })
                ),
            )
        }.flowOn(Dispatchers.IO)
    }

    fun close() {
        if (socket != null) {
            val msg = MessageHeader(Kind.GOODBYE, emptyList(), 0.toUInt()).encode()
            socket!!.getOutputStream().write(msg.toByteArray())
            socket!!.close()
        }
    }
}

@Serializable(with = TransportMode.Serializer::class)
enum class TransportMode {
    RER,
    METRO,
    TRAM,
    TRANSILIEN;

    object Serializer : KSerializer<TransportMode> {
        override val descriptor: SerialDescriptor
            get() = PrimitiveSerialDescriptor(
                "world.anhgelus.parismobility.data.TransportMode.Serializer",
                PrimitiveKind.BYTE
            )

        override fun serialize(
            encoder: Encoder,
            value: TransportMode
        ) {
            encoder.encodeByte(value.ordinal.toByte())
        }

        override fun deserialize(decoder: Decoder): TransportMode {
            val i = decoder.decodeByte().toInt()
            if (i >= entries.size) throw IllegalArgumentException("unknown transport mode")
            return entries[i]
        }
    }
}

data class MessageHeader(
    val kind: Kind,
    val flags: List<Flag>,
    val length: UInt,
) {
    @OptIn(ExperimentalSerializationApi::class)
    fun encode(): MutableList<Byte> {
        val ls = mutableListOf<Byte>()
        ls.add(kind.ordinal.toByte())
        flags.fold(0.toByte()) { acc, it ->
            acc.or(1.shl(it.ordinal).toByte())
        }.let { ls.add(it) }
        ByteBuffer.allocate(4)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(length.toInt())
            .array()
            .let { ls.addAll(it.toList()) }
        ls.add('\r'.code.toByte())
        ls.add('\n'.code.toByte())
        return ls
    }

    companion object {
        fun decode(input: InputStream): Pair<Kind, ByteArray> {
            var buf = ByteArray(8)
            var n = input.read(buf)
            if (n != buf.size) throw IllegalArgumentException("invalid message")
            val rawKind = buf[0]
            if (rawKind >= Kind.entries.size) throw IllegalArgumentException("unknown kind")
            val kind = Kind.entries[buf[0].toInt()]
            val rawFlags = buf[1]
            val flags = Flag.entries.fold(mutableListOf<Flag>()) { acc, it ->
                if (1.shl(it.ordinal).and(rawFlags.toInt()) != 0) acc.add(it)
                acc
            }
            val len = ByteBuffer.wrap(buf, 2, 4).getInt()
            buf = ByteArray(len)
            n = 0
            while (n < buf.size) {
                val nn = input.read(buf.sliceArray(n..<buf.size))
                if (nn < 0) throw IllegalArgumentException("invalid message")
                n += nn
            }
            if (flags.contains(Flag.GZIP))
                buf = GZIPInputStream(buf.inputStream()).readBytes()
            return Pair(kind, buf)
        }
    }
}

enum class Kind {
    RESPONSE,
    INVALID_REQUEST,
    INTERNAL_ERROR,
    DISRUPTIONS,
    MONITORING,
    GOODBYE,
}

enum class Flag {
    GZIP,
}

@Serializable
data class DisruptionsRequest(
    val kinds: List<TransportMode>,
    val lines: List<String>
)

@Serializable
data class MonitoringRequest(
    val stops: List<String>
)

@Serializable
data class ErrorResponse(
    val message: String,
    val error: String? = null
)