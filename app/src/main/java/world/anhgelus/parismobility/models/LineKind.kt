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
	val displayName: Int,
	val roundedCornerShape: CornerBasedShape,
	val logoId: Int? = null,
	val requiresBackground: Boolean = false,
) {
	METRO(
		R.string.metro,
		METRO_CORNER_SHAPE,
		R.drawable.metro,
	),
	RER(
		R.string.rer,
		TRAIN_CORNER_SHAPE,
		R.drawable.rer,
	),
	TRAM(
		R.string.tram,
		TRAM_CORNER_SHAPE,
		R.drawable.tram,
		true,
	),
	TRANSILIEN(
		R.string.transilien,
		TRAIN_CORNER_SHAPE,
		R.drawable.transilien,
	),
	BUS(
		R.string.bus,
		RoundedCornerShape(0),
	)
}