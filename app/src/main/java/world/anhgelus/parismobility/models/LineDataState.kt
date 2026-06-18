package world.anhgelus.parismobility.models

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import world.anhgelus.parismobility.R
import world.anhgelus.parismobility.ui.theme.CustomColorScheme

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
    val trafficQuality: TrafficQuality = TrafficQuality.NORMAL,
    val details: String? = null,
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

@Stable
enum class LineKind(
    val displayName: String,
    val prefix: String,
    val data: Int,
) {
    METRO("Métro", "metro", R.raw.metro),
    RER("RER", "rer", R.raw.rer),
    TRAM("Tram", "tram", R.raw.tram),
    TRANSILIEN("Transilien", "train", R.raw.transilien),
    BUS("Bus", "bus", R.raw.bus)
}

@Stable
enum class TrafficQuality(
    val color: @Composable (() -> Color?) = { null },
) {
    NORMAL,
    MINOR_ISSUE({ CustomColorScheme.warning }),
    MAJOR_ISSUE({ CustomColorScheme.error });
}