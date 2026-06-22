package world.anhgelus.parismobility.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import world.anhgelus.parismobility.data.Line
import world.anhgelus.parismobility.data.LineGroups
import world.anhgelus.parismobility.data.LineStops
import world.anhgelus.parismobility.data.MonitorStop
import world.anhgelus.parismobility.data.NetworkError
import world.anhgelus.parismobility.data.Result
import world.anhgelus.parismobility.data.SavedStop
import world.anhgelus.parismobility.data.Stop
import world.anhgelus.parismobility.data.get
import world.anhgelus.parismobility.models.LineKind
import world.anhgelus.parismobility.ui.theme.Typography
import kotlin.math.min

@Composable
fun StopsMonitoring(
    lines: LineGroups,
    stops: LineStops,
    savedStops: Collection<SavedStop>,
    monitoredStops: StateFlow<Map<Int, Result<MonitorStop, NetworkError>>>,
    modifier: Modifier = Modifier,
) {
    val monitor by monitoredStops.collectAsStateWithLifecycle()
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        val modifier = Modifier.padding(16.dp)
        SectionTitle("Prochains passages", modifier)
        savedStops.mapNotNull { stops[it] }.forEach { (line, stop) ->
            StopMonitoring(line.kind, lines[line]!!.second, stop, monitor[stop.id], modifier)
        }
    }
}

@Composable
fun StopMonitoring(
    kind: LineKind,
    line: Line,
    stop: Stop,
    monitor: Result<MonitorStop, NetworkError>?,
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
            Line(kind, line, {}, Modifier.size(48.dp))
            Text(
                text = stop.name,
                style = Typography.bodyMedium,
                modifier = Modifier.width(96.dp),
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                textAlign = TextAlign.Center,
            )
        }
        if (monitor != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                var error: NetworkError? = null
                var monit: MonitorStop? = null
                monitor.onError { error = it }.onSuccess { monit = it }
                if (error != null) {
                    Text(
                        text = error.displayError,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    val d = monit!!.compact(stop) {
                        it.journey.line.value.contains(stop.line)
                    }.entries.sortedBy { it.key }
                    if (d.isEmpty()) {
                        Text(
                            text = "Pas de service",
                            style = Typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        d.forEach {
                            Monitoring(it.key, it.value)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Monitoring(dest: String, monitor: MutableList<MonitorStop.StopVisit>) {
    Column(modifier = Modifier.offset(y = (-3).dp)) {
        Text(
            text = dest,
            style = Typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        FlowRow(
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            monitor.subList(0, min(monitor.size, 10)).forEach {
                val monit = it.journey.monitored
                when (monit.status) {
                    MonitorStop.Status.OnTime, MonitorStop.Status.Early, MonitorStop.Status.Delayed -> Text(
                        text = monit.displayTime(),
                        style = Typography.bodyMedium,
                    )

                    MonitorStop.Status.Cancelled -> Text(
                        text = "Annulé",
                        style = Typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )

                    MonitorStop.Status.Missed -> Text(
                        text = "Râté",
                        style = Typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )

                    MonitorStop.Status.Arrived -> Text(
                        text = "Arrivé",
                        style = Typography.bodyMedium
                    )

                    MonitorStop.Status.Departed -> Text(
                        text = "Parti",
                        style = Typography.bodyMedium
                    )

                    MonitorStop.Status.NotExpected -> Text(
                        text = "Non attendu",
                        style = Typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}