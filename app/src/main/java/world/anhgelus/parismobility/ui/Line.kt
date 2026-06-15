package world.anhgelus.parismobility.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import world.anhgelus.parismobility.R
import world.anhgelus.parismobility.ui.theme.Typography
import java.nio.file.WatchEvent

@Composable
fun Line(name: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_launcher_background),
            contentDescription = null
        )
        Text(text = name)
    }
}

@Composable
fun LineKind(name: String, lines: List<String>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = name, style = Typography.titleLarge)
        Lines(lines)
    }
}

@Composable
fun Lines(lines: List<String>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(lines) { Line(it) }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLines() {
    Lines(
        listOf(
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14"
        )
    )
}