package world.anhgelus.parismobility.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

enum class WidgetSize(
	val size: DpSize
) {
	SMALL(DpSize(100.dp, 100.dp)),
	MEDIUM(DpSize(200.dp, 150.dp)),
	LARGE(DpSize(350.dp, 250.dp));

	companion object {
		@Composable
		fun getSize(size: DpSize): WidgetSize = entries.reversed().first {
			size.height >= it.size.height && size.width >= it.size.width
		}
	}
}