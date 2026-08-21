package world.anhgelus.parismobility.data

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.Dispatchers
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
        socket =
            network.socketFactory.createSocket(BuildConfig.SERVER_HOSTNAME, BuildConfig.SERVER_PORT)
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
    val disruptions = flow {
        if (socket == null) {
            emit(Result.Error(NetworkError.NO_INTERNET))
            return@flow
        }
        val sock = socket!!
        val msg = MessageHeader(Kind.DISRUPTIONS, emptyList(), 0.toUInt()).encode()
        msg.addAll(
            Cbor.encodeToByteArray(DisruptionsRequest(emptyList(), emptyList()))
                .toList()
        )
        sock.getOutputStream().write(msg.toByteArray())
        val res = MessageHeader.decode(sock.getInputStream())
        var result: Result<Disruptions, NetworkError> = when (res.first) {
            Kind.RESPONSE -> Result.Ok(Cbor.decodeFromByteArray(res.second))
            Kind.INVALID_REQUEST -> Result.Error(NetworkError.UNKNOWN_ERROR)
            Kind.INTERNAL_ERROR, Kind.DISRUPTIONS, Kind.MONITORING -> Result.Error(NetworkError.SERVER_ERROR)
            Kind.GOODBYE -> throw IllegalArgumentException("Server disconnected")
        }
        emit(result)
    }
        .flowOn(Dispatchers.IO)

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
                "world.anhgelus.parismobility.data.Flag.Serializer",
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
            if (i >= entries.size) throw IllegalArgumentException("unknown flag")
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
            val n = input.read(buf)
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
            buf = ByteArray(len + 2)
            if (buf.size != input.read(buf)) throw IllegalArgumentException("invalid message")
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