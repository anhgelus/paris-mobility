package world.anhgelus.parismobility.data

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import world.anhgelus.parismobility.ui.theme.CustomColorScheme
import java.time.LocalDateTime
import java.time.ZoneOffset

typealias Disruptions = Map<String, List<Disruption>>

@Serializable(with = Severity.Serializer::class)
enum class Severity {
	INFORMATION,
	DISRUPT,
	BLOCKING;

	val color: Pair<Color, Color>
		@Composable
		get() = when (this) {
			INFORMATION -> Pair(Color.Transparent, MaterialTheme.colorScheme.onSurface)
			DISRUPT -> Pair(CustomColorScheme.warning, CustomColorScheme.onWarning)
			BLOCKING -> Pair(CustomColorScheme.error, CustomColorScheme.onError)
		}

	object Serializer : KSerializer<Severity> {
		override val descriptor: SerialDescriptor
			get() = PrimitiveSerialDescriptor(
				"world.anhgelus.parismobility.data.Severity.Serializer",
				PrimitiveKind.BYTE
			)

		override fun serialize(
			encoder: Encoder,
			value: Severity
		) {
			encoder.encodeByte(value.ordinal.toByte())
		}

		override fun deserialize(decoder: Decoder): Severity {
			val i = decoder.decodeByte().toInt()
			if (i >= entries.size) throw IllegalArgumentException("unknown severity")
			return entries[i]
		}
	}
}

@Stable
@Immutable
@Serializable
data class Period(
	@SerialName("begin") private val beginRaw: Long,
	@SerialName("end") private val endRaw: Long,
	@Transient val begin: LocalDateTime = LocalDateTime.ofEpochSecond(beginRaw, 0, ZoneOffset.UTC),
	@Transient val end: LocalDateTime = LocalDateTime.ofEpochSecond(endRaw, 0, ZoneOffset.UTC),
) : Comparable<Period> {
	override fun compareTo(other: Period): Int {
		return begin.compareTo(other.begin)
	}
}

@Stable
@Immutable
@Serializable
data class Disruption(
	val id: String,
	@SerialName("line_id") val lineId: String,
	val periods: List<Period>,
	val severity: Severity,
	val cause: String,
	val title: String,
	val message: String,
	@SerialName("short_message") val shortMessage: String? = null,
) : Comparable<Disruption> {
	fun isHappening(): Boolean {
		return periods.first().begin.isBefore(LocalDateTime.now())
	}

	override fun compareTo(other: Disruption): Int {
		if (isHappening() != other.isHappening()) return periods.first()
			.compareTo(other.periods.first())
		return -severity.compareTo(other.severity)
	}
}