package world.anhgelus.parismobility.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import world.anhgelus.parismobility.data.Disruption
import world.anhgelus.parismobility.ui.theme.Typography
import java.time.LocalDateTime

private fun Int.toStringDate(): String {
    var h = this.toString()
    if (h.length < 2) h = "0$h"
    return h
}

fun formatLocalDateTime(l: LocalDateTime): String {
    return "${l.dayOfMonth.toStringDate()}/${l.monthValue.toStringDate()}/${l.year.toStringDate()}"
}

@Composable
fun DisruptionCard(disruption: Disruption, modifier: Modifier = Modifier) {
    val color = disruption.severity.color
    val enabled = disruption.isHappening()
    Card(
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainer),
        colors = CardDefaults.cardColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
            containerColor =
                if (enabled) MaterialTheme.colorScheme.surfaceContainerHigh
                else MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.elevatedCardElevation(),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        val modifier = Modifier.padding(16.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (enabled) color.first else MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(16.dp)
        ) {
            Column {
                SectionTitle(
                    content = disruption.title,
                    color = if (enabled) color.second else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                val p = disruption.periods.first()
                Text(
                    text = "Du ${formatLocalDateTime(p.begin)} au ${formatLocalDateTime(p.end)}",
                    style = Typography.bodySmall,
                    color = if (enabled) color.second else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Text(AnnotatedString.fromHtml(disruption.message), modifier)
    }
}