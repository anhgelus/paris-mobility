package world.anhgelus.parismobility.models

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Stable
import world.anhgelus.parismobility.R
import world.anhgelus.parismobility.data.Disruption

@Stable
data class LinesGroupDataState(
    val kind: LineKind,
    val lines: List<LineDataState>,
)

@Stable
data class LineDataState(
    val name: String,
    val id: String,
    val resource: Int,
    val kind: LineKind,
    val disruption: Disruption? = null,
) : Comparable<LineDataState> {
    private val metroBisSuffix = " Bis"
    private val tramPrefix = "T"

    override fun compareTo(other: LineDataState): Int {
        if (kind != other.kind) throw IllegalArgumentException("must have the same kind")
        return when (kind) {
            LineKind.TRAM -> {
                var na = name.removePrefix(tramPrefix)
                var nb = other.name.removePrefix(tramPrefix)
                if (!na.last().isDigit()) na = na.removeSuffix(na.last().toString())
                if (!nb.last().isDigit()) nb = nb.removeSuffix(nb.last().toString())
                val ln = na.length - nb.length
                if (ln != 0) ln
                else String.CASE_INSENSITIVE_ORDER.compare(name, other.name)
            }

            else -> {
                val ln = name.removeSuffix(metroBisSuffix).length - other.name.removeSuffix(
                    metroBisSuffix
                ).length
                if (ln != 0) ln
                else String.CASE_INSENSITIVE_ORDER.compare(name, other.name)
            }
        }
    }
}

private val TRAIN_CORNER_SHAPE = RoundedCornerShape(20)
private val METRO_CORNER_SHAPE = RoundedCornerShape(50)
private val TRAM_CORNER_SHAPE = RoundedCornerShape(5)

@Stable
enum class LineKind(
    val displayName: String,
    val prefix: String,
    val data: Int,
    val roundedCornerShape: CornerBasedShape,
    val requiresBackground: Boolean = false,
) {
    METRO(
        "Métro",
        "metro",
        R.raw.metro,
        METRO_CORNER_SHAPE,
    ),
    RER(
        "RER",
        "rer",
        R.raw.rer,
        TRAIN_CORNER_SHAPE,
    ),
    TRAM(
        "Tram",
        "tram",
        R.raw.tram,
        TRAM_CORNER_SHAPE,
        true,
    ),
    TRANSILIEN(
        "Transilien",
        "train",
        R.raw.transilien,
        TRAIN_CORNER_SHAPE,
    ),
    BUS(
        "Bus",
        "bus",
        R.raw.bus,
        RoundedCornerShape(0),
    )
}