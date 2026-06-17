package world.anhgelus.parismobility.models

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import world.anhgelus.parismobility.ui.theme.CustomColorScheme

@Stable
data class LineDataState(
    val name: String,
    val resource: Int,
    val trafficQuality: TrafficQuality = TrafficQuality.NORMAL,
    val details: String? = null,
)

enum class TrafficQuality(
    val color: @Composable (() -> Color?) = { null },
) {
    NORMAL,
    MINOR_ISSUE({ CustomColorScheme.warning }),
    MAJOR_ISSUE({ CustomColorScheme.error });
}