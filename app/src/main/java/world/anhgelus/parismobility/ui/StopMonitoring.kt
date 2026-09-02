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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import world.anhgelus.parismobility.R
import world.anhgelus.parismobility.data.Line
import world.anhgelus.parismobility.data.LineGroups
import world.anhgelus.parismobility.data.LineState
import world.anhgelus.parismobility.data.MonitoringStop
import world.anhgelus.parismobility.data.MonitoringStops
import world.anhgelus.parismobility.data.STOPS
import world.anhgelus.parismobility.data.SavedStop
import world.anhgelus.parismobility.data.Status
import world.anhgelus.parismobility.data.Stop
import world.anhgelus.parismobility.data.get
import world.anhgelus.parismobility.models.LineKind
import world.anhgelus.parismobility.ui.theme.Typography
import java.time.ZonedDateTime
import kotlin.math.min

@Composable
fun StopsMonitoring(
	lines: LineGroups,
	savedStops: Collection<SavedStop>,
	monitoredStops: StateFlow<MonitoringStops>,
	onLineClicked: (LineKind, Line) -> Unit,
	modifier: Modifier = Modifier,
) {
	val monitor by monitoredStops.collectAsStateWithLifecycle()
	Card(
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.secondaryContainer,
			contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
		),
		modifier = modifier
			.fillMaxWidth()
			.padding(top = 8.dp),
		elevation = CardDefaults.cardElevation(1.dp),
	) {
		val modifier = Modifier.padding(16.dp)
		SectionTitle(stringResource(R.string.home_next_trains), modifier)
		if (savedStops.isEmpty()) {
			Text(stringResource(R.string.home_no_stops), modifier)
			return@Card
		}
		savedStops.mapNotNull { STOPS[it.line.line]?.get(it.stop)?.let { s -> Pair(it.line, s) } }
			.forEach { (line, stop) ->
				val l = lines[line]!!.second
				StopMonitoring(
					line.kind,
					l,
					stop,
					monitor.map[stop.zda.toString()],
					{ onLineClicked(line.kind, l.line) },
					modifier
				)
			}
	}
}

@Composable
fun StopMonitoring(
	kind: LineKind,
	line: LineState,
	stop: Stop,
	monitor: List<MonitoringStop>?,
	onLineClicked: () -> Unit,
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
			Line(kind, line, onLineClicked, Modifier.size(48.dp))
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
			if (monitor.isNullOrEmpty()) {
				Text(
					text = stringResource(R.string.home_train_nothing),
					style = Typography.bodyMedium,
					fontWeight = FontWeight.Bold
				)
			} else {
				val now = ZonedDateTime.now()
				monitor.filter { it.line == line.line.id }
					.filter { it.time.isAfter(now) }
					// remove trains that arrive at this stop
					.filter { !it.destination.contains(stop.name) }
					.fold(mutableMapOf<String, MutableList<MonitoringStop>>()) { acc, t ->
						acc.also {
							t.destination.first().let { k ->
								acc[k] = acc[k]?.also { it.add(t) } ?: mutableListOf(t)
							}
						}
					}.forEach { (dest, it) -> Monitor(dest, it) }
			}
		}
	}
}

@Composable
fun Monitor(dest: String, monitor: MutableList<MonitoringStop>) {
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
				when (it.status) {
					Status.ON_TIME, Status.EARLY, Status.DELAYED -> Text(
						text = it.displayTime,
						style = Typography.bodyMedium,
					)

					Status.CANCELLED -> Text(
						text = stringResource(R.string.home_train_cancelled),
						style = Typography.bodyMedium,
						color = MaterialTheme.colorScheme.error
					)

					Status.MISSED -> Text(
						text = stringResource(R.string.home_train_missed),
						style = Typography.bodyMedium,
						color = MaterialTheme.colorScheme.error
					)

					Status.ARRIVED -> Text(
						text = stringResource(R.string.home_train_arrived),
						style = Typography.bodyMedium
					)

					Status.DEPARTED -> Text(
						text = stringResource(R.string.home_train_departed),
						style = Typography.bodyMedium
					)

					Status.NOT_EXPECTED -> Text(
						text = stringResource(R.string.home_train_not_expected),
						style = Typography.bodyMedium,
						color = MaterialTheme.colorScheme.error
					)
				}
			}
		}
	}
}