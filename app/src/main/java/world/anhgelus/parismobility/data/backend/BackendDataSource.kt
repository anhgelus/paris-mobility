package world.anhgelus.parismobility.data.backend

import android.net.ConnectivityManager
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
import world.anhgelus.parismobility.data.Disruptions
import world.anhgelus.parismobility.data.MonitoringStops
import world.anhgelus.parismobility.data.NetworkError
import world.anhgelus.parismobility.data.Result
import world.anhgelus.parismobility.data.Stop
import java.net.SocketException

class BackendDataSource(
    private val conn: BackendConnection,
) : ConnectivityManager.NetworkCallback() {
    @OptIn(ExperimentalSerializationApi::class)
    private suspend inline fun <reified R, reified T> get(
        kind: Kind,
        req: R,
    ): Result<T, NetworkError> {
        val socket = conn.socket.receive()
        if (socket == null || !socket.isConnected) return Result.Error(NetworkError.NO_INTERNET)
        val req = Cbor.encodeToByteArray(req)
        try {
            val res = conn.send(Message(kind, emptyList()), req)
                ?: return Result.Error(NetworkError.SERVER_ERROR)
            return when (res.first) {
                Kind.RESPONSE -> Result.Ok(Cbor.Default.decodeFromByteArray(res.second))
                Kind.INVALID_REQUEST -> Result.Error(
                    NetworkError.UNKNOWN_ERROR,
                    Cbor.decodeFromByteArray<ErrorResponse>(res.second).let {
                        it.error?.let { m -> Log.w("BackendData", m) }
                        it.message
                    }
                )

                Kind.INTERNAL_ERROR -> Result.Error(NetworkError.SERVER_ERROR)
                Kind.GOODBYE -> throw IllegalArgumentException("Server disconnected")
                else -> throw IllegalArgumentException("invalid kind for response")
            }
        } catch (e: SocketException) {
            conn.close()
            return Result.Error(NetworkError.SERVER_ERROR, e.message)
        }
    }

    val disruptions = flow {
        get<DisruptionsRequest, Disruptions>(
            Kind.DISRUPTIONS,
            DisruptionsRequest(emptyList(), emptyList()),
        ).let { emit(it) }
    }.flowOn(Dispatchers.IO)

    fun monitorStops(stops: Collection<Stop>): Flow<Result<MonitoringStops, NetworkError>> {
        return flow {
            get<MonitoringRequest, MonitoringStops>(
                Kind.MONITORING,
                MonitoringRequest(stops.map { it.zda.toString() })
            ).let { emit(it) }
        }.flowOn(Dispatchers.IO)
    }

    fun close() {
        conn.socket.tryReceive().getOrNull()?.let { socket ->
            Message(Kind.GOODBYE, emptyList()).encode(ByteArray(0)).let {
                socket.getOutputStream().write(it)
            }
        }
        conn.close()
    }
}

private typealias Kind = Message.Kind

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

@Serializable
private data class DisruptionsRequest(
    val kinds: List<TransportMode>,
    val lines: List<String>
)

@Serializable
private data class MonitoringRequest(
    val stops: List<String>
)

@Serializable
private data class ErrorResponse(
    val message: String,
    val error: String? = null
)