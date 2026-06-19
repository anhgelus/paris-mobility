package world.anhgelus.parismobility.models

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Stable

private val TRAIN_CORNER_SHAPE = RoundedCornerShape(20)
private val METRO_CORNER_SHAPE = RoundedCornerShape(50)
private val TRAM_CORNER_SHAPE = RoundedCornerShape(5)

@Stable
enum class LineKind(
    val displayName: String,
    val prefix: String,
    val roundedCornerShape: CornerBasedShape,
    val requiresBackground: Boolean = false,
) {
    METRO(
        "Métro",
        "metro",
        METRO_CORNER_SHAPE,
    ),
    RER(
        "RER",
        "rer",
        TRAIN_CORNER_SHAPE,
    ),
    TRAM(
        "Tram",
        "tram",
        TRAM_CORNER_SHAPE,
        true,
    ),
    TRANSILIEN(
        "Transilien",
        "train",
        TRAIN_CORNER_SHAPE,
    ),
    BUS(
        "Bus",
        "bus",
        RoundedCornerShape(0),
    )
}