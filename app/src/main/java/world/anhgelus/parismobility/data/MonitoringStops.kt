package world.anhgelus.parismobility.data

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

typealias MonitoringMap = Map<String, List<MonitoringStop>>

@Stable
@Immutable
data class MonitoringStops(
	val map: MonitoringMap,
	val at: ZonedDateTime = ZonedDateTime.now(),
	val synced: Boolean = false,
) {
	fun update(): MonitoringStops {
		val at = ZonedDateTime.now()
		return MonitoringStops(map.mapValues { (_, stop) ->
			stop.filter { it.time.isAfter(at) }.map { it.update() }
		}.filter { (_, stop) -> stop.isNotEmpty() }, at)
	}
}

@Serializable(with = Status.Serializer::class)
enum class Status {
	ON_TIME,
	EARLY,
	DELAYED,
	CANCELLED,
	MISSED,
	ARRIVED,
	DEPARTED,
	NOT_EXPECTED;

	object Serializer : KSerializer<Status> {
		override val descriptor: SerialDescriptor
			get() = PrimitiveSerialDescriptor(
				"world.anhgelus.parismobility.data.Status.Serializer",
				PrimitiveKind.BYTE
			)

		override fun serialize(
			encoder: Encoder,
			value: Status
		) {
			encoder.encodeByte(value.ordinal.toByte())
		}

		override fun deserialize(decoder: Decoder): Status {
			val i = decoder.decodeByte().toInt()
			if (i >= entries.size) throw IllegalArgumentException("unknown status")
			return entries[i]
		}
	}
}

@Serializable(with = VehicleFeature.Serializer::class)
enum class VehicleFeature {
	LONG_TRAIN,
	SHORT_TRAIN;

	object Serializer : KSerializer<VehicleFeature> {
		override val descriptor: SerialDescriptor
			get() = PrimitiveSerialDescriptor(
				"world.anhgelus.parismobility.data.TrainFeature.Serializer",
				PrimitiveKind.BYTE
			)

		override fun serialize(
			encoder: Encoder,
			value: VehicleFeature
		) {
			encoder.encodeByte(value.ordinal.toByte())
		}

		override fun deserialize(decoder: Decoder): VehicleFeature {
			val i = decoder.decodeByte().toInt()
			if (i >= entries.size) throw IllegalArgumentException("unknown feature")
			return entries[i]
		}
	}
}

@Stable
@Immutable
@Serializable
data class MonitoringStop(
	@SerialName("is_stopped") val isStopped: Boolean,
	val destination: List<String>,
	@SerialName("time") private val rawTime: Long,
	val status: Status,
	@SerialName("vehicle_feature") val vehicleFeatures: List<VehicleFeature>? = null,
	@Transient val time: ZonedDateTime = ZonedDateTime.ofInstant(
		Instant.ofEpochSecond(rawTime),
		ZoneId.systemDefault()
	),
	@Transient val displayTime: String = displayTime(isStopped, time)
) {
	fun update(): MonitoringStop {
		return MonitoringStop(isStopped, destination, rawTime, status, vehicleFeatures)
	}
}

fun displayTime(isStopped: Boolean, time: ZonedDateTime): String {
	if (isStopped) return "À quai"
	return when (val mins = ChronoUnit.MINUTES.between(ZonedDateTime.now(), time)) {
		0L -> "À l'approche"
		in 1..45 -> "$mins min"

		else -> {
			val h = if (time.hour < 10) "0${time.hour}" else time.hour
			val m = if (time.minute < 10) "0${time.minute}" else time.minute
			"$h:$m"
		}
	}
}