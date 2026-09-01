package world.anhgelus.parismobility.data.backend

import kotlinx.coroutines.flow.StateFlow
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream
import kotlin.experimental.or

interface Connection {
	suspend fun send(msg: Message, body: ByteArray): Pair<Message.Kind, ByteArray>?
	fun close()

	val isConnected: StateFlow<Boolean>
}

data class Message(
	val kind: Kind,
	val flags: List<Flag>,
) {
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

	enum class Flag {
		GZIP,
	}

	enum class Kind {
		RESPONSE,
		INVALID_REQUEST,
		INTERNAL_ERROR,
		DISRUPTIONS,
		MONITORING,
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