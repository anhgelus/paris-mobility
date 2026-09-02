package world.anhgelus.parismobility.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import world.anhgelus.parismobility.R
import world.anhgelus.parismobility.data.LineGroups
import world.anhgelus.parismobility.data.LineState
import world.anhgelus.parismobility.data.MonitoringStop
import world.anhgelus.parismobility.data.MonitoringStops
import world.anhgelus.parismobility.data.STOPS
import world.anhgelus.parismobility.data.SavedStop
import world.anhgelus.parismobility.data.Stop
import world.anhgelus.parismobility.data.get
import world.anhgelus.parismobility.models.LineKind
import world.anhgelus.parismobility.ui.convertMonitor
import world.anhgelus.parismobility.ui.displayStop
import kotlin.math.min

@Composable
fun StopsMonitoring(
	ctx: Context,
	darkMode: Boolean,
	lines: LineGroups,
	savedStops: Collection<SavedStop>,
	monitor: MonitoringStops,
	modifier: GlanceModifier = GlanceModifier
) {
	Column(modifier = modifier) {
		savedStops.mapNotNull { STOPS[it.line.line]?.get(it.stop)?.let { s -> Pair(it.line, s) } }
			.forEach { (line, stop) ->
				val l = lines[line]!!.second
				StopMonitoring(
					ctx,
					darkMode,
					line.kind,
					l,
					stop,
					monitor.map[stop.zda.toString()],
					GlanceModifier.padding(top = 12.dp)
				)
			}
	}
}

@Composable
fun StopMonitoring(
	ctx: Context,
	darkMode: Boolean,
	kind: LineKind,
	line: LineState,
	stop: Stop,
	monitor: List<MonitoringStop>?,
	modifier: GlanceModifier = GlanceModifier
) {
	Row(
		verticalAlignment = Alignment.Top,
		modifier = modifier,
	) {
		val textStyle = TextStyle(GlanceTheme.colors.onSurface)
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			modifier = GlanceModifier.width(96.dp)
		) {
			Line(darkMode, kind, line, GlanceTheme.colors.surface)
			Text(text = stop.name, style = textStyle, maxLines = 1)
		}
		Column(GlanceModifier.padding(start = 8.dp)) {
			val monitor = monitor?.let { convertMonitor(stop, line.line, it) }
			if (monitor.isNullOrEmpty()) {
				Text(ctx.getString(R.string.home_train_nothing), style = textStyle)
			} else {
				monitor.forEach { (destination, monitor) ->
					Text(destination, style = textStyle.copy(fontWeight = FontWeight.Bold))
					Row(GlanceModifier.padding(bottom = 16.dp)) {
						monitor.subList(0, min(monitor.size, 5)).forEach {
							displayStop(ctx, it).let { (v, err) ->
								Text(
									text = v,
									style = TextStyle(
										color = if (err) GlanceTheme.colors.error
										else GlanceTheme.colors.onSurface
									),
									modifier = GlanceModifier.padding(top = 4.dp)
										.padding(end = 8.dp)
								)
							}
						}
					}
				}
			}
		}
	}
}