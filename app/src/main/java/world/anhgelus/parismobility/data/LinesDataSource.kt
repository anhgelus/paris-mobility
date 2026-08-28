package world.anhgelus.parismobility.data

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

object LinesDataSource {
	fun getLines(): Map<Line.TransportMode, List<LineState>> {
		return mutableMapOf<Line.TransportMode, List<LineState>>().let { res ->
			MODES.forEach { (k, map) ->
				res[k] = map.values.map { LineState(it) }
			}
			res
		}
	}
}

@Serializable
data class LineState(
	val line: Line,
	@Transient private val _disruptionSeverity: MutableState<Severity> = mutableStateOf(Severity.INFORMATION),
) : Comparable<LineState> {
	val disruptionSeverity: State<Severity> = _disruptionSeverity

	fun setDisruption(sev: Severity?): LineState {
		_disruptionSeverity.value = sev ?: Severity.INFORMATION
		return this
	}

	private val metroBisSuffix = "B"
	private val tramPrefix = "T"

	override fun compareTo(other: LineState): Int {
		if (line.mode != other.line.mode) throw IllegalArgumentException("must have the same kind")
		return when (line.mode) {
			Line.TransportMode.TRAM -> {
				var na = line.name.removePrefix(tramPrefix)
				var nb = other.line.name.removePrefix(tramPrefix)
				if (!na.last().isDigit()) na = na.removeSuffix(na.last().toString())
				if (!nb.last().isDigit()) nb = nb.removeSuffix(nb.last().toString())
				val ln = na.length - nb.length
				if (ln != 0) ln
				else String.CASE_INSENSITIVE_ORDER.compare(line.name, other.line.name)
			}

			Line.TransportMode.RAIL -> String.CASE_INSENSITIVE_ORDER.compare(
				line.name,
				other.line.name
			)

			else -> {
				val ln =
					line.name.removeSuffix(metroBisSuffix).length - other.line.name.removeSuffix(
						metroBisSuffix
					).length
				if (ln != 0) ln
				else String.CASE_INSENSITIVE_ORDER.compare(line.name, other.line.name)
			}
		}
	}
}