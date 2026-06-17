package world.anhgelus.parismobility.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import world.anhgelus.parismobility.models.LineDataState

@Composable
fun Line(data: LineDataState, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(data.resource),
        contentDescription = data.name,
        modifier = modifier.border(
            width = 4.dp,
            color = data.trafficQuality.color() ?: Color.Transparent,
            shape = RoundedCornerShape(20)
        ),
    )
}

@Composable
fun LineKind(name: String, lines: List<LineDataState>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionTitle(name)
        Lines(lines)
    }
}

@Composable
fun Lines(lines: List<LineDataState>, modifier: Modifier = Modifier) {
    val size = 48.dp
    val items = 6
    FlowRow(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
        maxItemsInEachRow = items
    ) {
        lines.forEach { Line(it, Modifier.size(size)) }
        val mod = lines.size % items
        if (mod != 0)
            repeat(items - (lines.size % items)) { Spacer(Modifier.size(size)) }
    }
}