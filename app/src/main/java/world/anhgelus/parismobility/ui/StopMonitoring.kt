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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import world.anhgelus.parismobility.ui.theme.Typography

@Preview(showBackground = true)
@Composable
fun StopsMonitoring(modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        val modifier = Modifier.padding(16.dp)
        Text(
            text = "Prochains passages",
            style = Typography.titleMedium,
            modifier = modifier,
        )
        StopMonitoring(
            "A",
            "Reuil-Malmaison",
            mapOf(
                Pair("Marne-la-Vallée", listOf("3 min", "8 min")),
                Pair("Boissy-Saint-Léger", listOf("5 min", "12 min")),
            ),
            modifier,
        )
        StopMonitoring(
            "7",
            "Jussieu",
            mapOf(
                Pair("La Courneuve", listOf("3 min", "8 min", "13 min")),
            ),
            modifier
        )
        StopMonitoring(
            "J",
            "Gare Saint-Lazare",
            mapOf(
                Pair("Gisors", listOf("23 min")),
                Pair("Mantes-la-Jolie", listOf("18 min", "37 min")),
                Pair("Pontoise", listOf("10 min", "45 min")),
            ),
            modifier
        )
    }
}

@Composable
fun StopMonitoring(
    line: String,
    stop: String,
    data: Map<String, List<String>>,
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
            Line(name = line, Modifier.size(48.dp))
            Text(
                text = stop,
                style = Typography.bodyMedium,
                modifier = Modifier.width(92.dp),
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                textAlign = TextAlign.Center,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            data.forEach { (k, v) ->
                Monitoring(k, v)
            }
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