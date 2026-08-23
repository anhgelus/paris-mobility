package world.anhgelus.parismobility.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter

@Composable
fun Image(
    painter: Painter,
    contentDescription: String,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 1f,
) {
    var scale by remember {
        mutableFloatStateOf(1.5f)
    }
    var offset by remember {
        mutableStateOf(Offset.Zero)
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val state = rememberTransformableState { _, zoomChange, panChange, _ ->
            scale = (scale * zoomChange).coerceIn(1f..5f)

            val min = minOf(constraints.maxWidth, constraints.maxHeight)

            val maxX = maxOf((min * scale * aspectRatio - constraints.maxWidth) / 2, 0f)
            val maxY = maxOf((min * scale / aspectRatio - constraints.maxHeight) / 2, 0f)

            offset = Offset(
                x = (offset.x + scale * panChange.x).coerceIn(-maxX..maxX),
                y = (offset.y + scale * panChange.y).coerceIn(-maxY..maxY)
            )
        }
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .transformable(state),
        )
    }
}