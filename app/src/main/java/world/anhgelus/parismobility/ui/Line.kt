package world.anhgelus.parismobility.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import world.anhgelus.parismobility.data.Line
import world.anhgelus.parismobility.data.Severity
import world.anhgelus.parismobility.models.LineKind

@Composable
fun Line(kind: LineKind, line: Line, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val sev = line.disruptionSeverity
    var modifier = modifier
    if (sev != Severity.INFORMATION) {
        modifier = modifier
            .border(
                width = 2.dp,
                color = sev.color.first,
                shape = kind.roundedCornerShape
            )
            .padding(8.dp)
    }
    Box(
        modifier = modifier.clickable(onClick = onClick),
    ) {
        var modifier: Modifier = Modifier
        if (sev == Severity.INFORMATION) {
            modifier = modifier.padding(4.dp)
        }
        Image(
            painter = painterResource(line.resource),
            contentDescription = line.name,
            modifier = modifier
                .background(
                    color =
                        if (isSystemInDarkTheme() && kind.requiresBackground) Color.White
                        else Color.Transparent,
                    shape = kind.roundedCornerShape
                ),
        )
    }
}

@Composable
fun LineDetailed(kind: LineKind, line: Line, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = painterResource(line.resource),
            contentDescription = line.name,
            modifier = Modifier
                .size(48.dp)
                .background(
                    color =
                        if (isSystemInDarkTheme() && kind.requiresBackground) Color.White
                        else Color.Transparent,
                    shape = kind.roundedCornerShape
                ),
        )
        Text("${kind.displayName} ${line.name}")
    }
}

@Composable
fun LineKind(
    name: String,
    kind: LineKind,
    lines: Collection<Line>,
    onClick: (LineKind, Line) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionTitle(name)
        Lines(kind, lines, onClick)
    }
}

@Composable
fun Lines(
    kind: LineKind,
    lines: Collection<Line>,
    onClick: (LineKind, Line) -> Unit,
    modifier: Modifier = Modifier
) {
    val size = 48.dp
    val items = 6
    FlowRow(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
        maxItemsInEachRow = items
    ) {
        lines.forEach { Line(kind, it, onClick = { onClick(kind, it) }, Modifier.size(size)) }
        val mod = lines.size % items
        if (mod != 0)
            repeat(items - (lines.size % items)) { Spacer(Modifier.size(size)) }
    }
}