package world.anhgelus.parismobility.data.backend

import android.net.ConnectivityManager
import android.util.Log
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import world.anhgelus.parismobility.data.Disruptions
import world.anhgelus.parismobility.data.MonitoringMap
import world.anhgelus.parismobility.data.Stop
import java.net.SocketException

class BackendDataSource(
	private val conn: Connection,
) : ConnectivityManager.NetworkCallback() {
	val isConnected = conn.isConnected

	@OptIn(ExperimentalSerializationApi::class)
	private suspend inline fun <reified R, reified T> get(
		kind: Kind,
		req: R,
	): Result<T> {
		val req = Cbor.encodeToByteArray(req)
		try {
			val res = conn.send(Message(kind, emptyList()), req)
				?: return Result.failure(ConnectionException.Kind.NOT_CONNECTED.exception)
			return when (res.first) {
				Kind.RESPONSE -> Result.success(Cbor.decodeFromByteArray(res.second))
				Kind.INVALID_REQUEST -> {
					Cbor.decodeFromByteArray<ErrorResponse>(res.second).let {
						it.error?.let { m -> Log.w("BackendData", m) }
					}
					Result.failure(ConnectionException.Kind.INVALID_DATA.exception)
				}

				Kind.INTERNAL_ERROR -> Result.failure(ConnectionException.Kind.SERVER_ERROR.exception)
				else -> Result.failure(IllegalArgumentException("invalid kind for response"))
			}
		} catch (e: SocketException) {
			conn.close()
			e.message?.let { Log.w("BackendData", it) }
			return Result.failure(ConnectionException.Kind.SERVER_ERROR.exception)
		} catch (e: SerializationException) {
			e.message?.let { Log.w("BackendData", it) }
			return Result.failure(ConnectionException.Kind.SERVER_ERROR.exception.initCause(e))
		}
	}

	suspend fun disruptions() = get<DisruptionsRequest, Disruptions>(
		Kind.DISRUPTIONS,
		DisruptionsRequest(emptyList(), emptyList()),
	)

	suspend fun monitorStops(stops: Collection<Stop>) = get<MonitoringRequest, MonitoringMap>(
		Kind.MONITORING,
		MonitoringRequest(stops.map { it.zda.toString() })
	)

	fun close() {
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

data class ConnectionException(
	val kind: Kind,
) : Exception() {
	override val message: String = kind.displayError

	enum class Kind(
		val displayError: String
	) {
		SERVER_ERROR("Serverside error"),
		INVALID_DATA("Data sent is invalid"),
		RATE_LIMITED("Rate limited by the server"),
		NOT_CONNECTED("Not connected to the server. Check your internet connection"),
		UNKNOWN_ERROR("Unknown error");

		val exception = ConnectionException(this)
	}
}