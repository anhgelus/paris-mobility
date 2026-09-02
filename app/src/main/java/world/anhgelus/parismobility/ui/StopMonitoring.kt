package world.anhgelus.parismobility.ui

import android.content.Context
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
			val monitor = monitor?.let { convertMonitor(stop, line.line, it) }
			if (monitor.isNullOrEmpty()) {
				Text(
					text = stringResource(R.string.home_train_nothing),
					style = Typography.bodyMedium,
					fontWeight = FontWeight.Bold
				)
			} else {
				monitor.forEach { (destination, it) -> Monitor(destination, it) }
			}
		}
	}
}

@Composable
fun Monitor(destination: String, monitor: List<MonitoringStop>) {
	Column(modifier = Modifier.offset(y = (-3).dp)) {
		Text(
			text = destination,
			style = Typography.bodyMedium,
			fontWeight = FontWeight.Bold
		)
		FlowRow(
			verticalArrangement = Arrangement.Center,
			horizontalArrangement = Arrangement.spacedBy(16.dp),
		) {
			monitor.subList(0, min(monitor.size, 10)).forEach {
				displayStop(LocalContext.current, it).let { (v, err) ->
					Text(
						text = v,
						style = Typography.bodyMedium,
						color = if (err) MaterialTheme.colorScheme.error else Color.Unspecified,
					)
				}
			}
		}
	}
}

fun displayStop(ctx: Context, stop: MonitoringStop): Pair<String, Boolean> = when (stop.status) {
	Status.ON_TIME, Status.EARLY, Status.DELAYED -> stop.displayTime to false
	Status.CANCELLED -> ctx.getString(R.string.home_train_cancelled) to true
	Status.MISSED -> ctx.getString(R.string.home_train_missed) to true
	Status.ARRIVED -> ctx.getString(R.string.home_train_arrived) to false
	Status.DEPARTED -> ctx.getString(R.string.home_train_departed) to false
	Status.NOT_EXPECTED -> ctx.getString(R.string.home_train_not_expected) to true
}

fun convertMonitor(
	stop: Stop,
	line: Line,
	monitor: List<MonitoringStop>
): Map<String, List<MonitoringStop>> {
	val now = ZonedDateTime.now()
	return monitor.filter { it.line == line.id }
		.filter { it.time.isAfter(now) }
		// remove trains that arrive at this stop
		.filter { b ->
			b.destination.map { it.lowercase() }.let {
				!it.any { s -> s.contains(stop.name, true) } &&
					!it.any { s -> stop.name.contains(s, true) }
			}
		}
		.fold(mutableMapOf<String, MutableList<MonitoringStop>>()) { acc, t ->
			acc.also {
				t.destination.first().let { k ->
					acc[k] = acc[k]?.also { it.add(t) } ?: mutableListOf(t)
				}
			}
		}
}