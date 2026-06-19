package world.anhgelus.parismobility.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import world.anhgelus.parismobility.models.StopDataState
import world.anhgelus.parismobility.ui.theme.Typography

@Composable
fun StopsMonitoring(stops: List<StopDataState>, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        val modifier = Modifier.padding(16.dp)
        SectionTitle("Prochains passages", modifier)
        stops.forEach { StopMonitoring(it, modifier) }
    }
}

@Composable
fun StopMonitoring(
    stop: StopDataState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Line(stop.kind, stop.line, Modifier.size(48.dp))
            Text(
                text = stop.name,
                style = Typography.bodyMedium,
                modifier = Modifier.width(96.dp),
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                textAlign = TextAlign.Center,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            stop.nextTrains.forEach { (k, v) -> Monitoring(k, v) }
        }
    }
}

@Composable
fun Monitoring(direction: String, times: List<String>) {
    Column(modifier = Modifier.offset(y = (-3).dp)) {
        Text(text = direction, style = Typography.bodyMedium, fontWeight = FontWeight.Bold)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            times.forEach { Text(text = it, style = Typography.bodyMedium) }
        }
    }
}