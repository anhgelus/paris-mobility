package world.anhgelus.parismobility.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import world.anhgelus.parismobility.R

@Composable
fun Line(name: String, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_launcher_background),
        contentDescription = name,
        modifier = modifier,
    )
}

@Composable
fun LineKind(name: String, lines: List<String>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionTitle(name)
        Lines(lines)
    }
}

@Composable
fun Lines(lines: List<String>) {
    val size = 48.dp
    val items = 6
    FlowRow(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
        maxItemsInEachRow = items
    ) {
        lines.forEach { Line(it, Modifier.size(size)) }
        val mod = lines.size % items
        if (mod != 0)
            repeat(items - (lines.size % items)) { Spacer(Modifier.size(size)) }
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