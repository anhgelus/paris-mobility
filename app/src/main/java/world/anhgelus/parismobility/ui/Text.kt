package world.anhgelus.parismobility.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import world.anhgelus.parismobility.ui.theme.Typography

@Composable
fun ScreenTitle(
    content: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    Text(
        text = content,
        color = color,
        style = Typography.headlineMedium,
        modifier = modifier.padding(top = 24.dp),
    )
}

@Composable
fun SectionTitle(
    content: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    Text(
        text = content,
        style = Typography.titleLarge,
        modifier = modifier,
        color = color,
    )
}