package world.anhgelus.parismobility.models

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Stable
import world.anhgelus.parismobility.R

private val TRAIN_CORNER_SHAPE = RoundedCornerShape(20)
private val METRO_CORNER_SHAPE = RoundedCornerShape(50)
private val TRAM_CORNER_SHAPE = RoundedCornerShape(5)

@Stable
enum class LineKind(
    val displayName: String,
    val roundedCornerShape: CornerBasedShape,
    val logoId: Int? = null,
    val requiresBackground: Boolean = false,
) {
    METRO(
        "Métro",
        METRO_CORNER_SHAPE,
        R.drawable.metro,
    ),
    RER(
        "RER",
        TRAIN_CORNER_SHAPE,
        R.drawable.rer,
    ),
    TRAM(
        "Tram",
        TRAM_CORNER_SHAPE,
        R.drawable.tram,
        true,
    ),
    TRANSILIEN(
        "Transilien",
        TRAIN_CORNER_SHAPE,
        R.drawable.transilien,
    ),
    BUS(
        "Bus",
        RoundedCornerShape(0),
    )
}