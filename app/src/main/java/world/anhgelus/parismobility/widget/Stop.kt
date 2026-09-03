package world.anhgelus.parismobility.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.lazy.LazyListScope
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
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

fun LazyListScope.stopsMonitoring(
	ctx: Context,
	darkMode: Boolean,
	lines: LineGroups,
	savedStops: Collection<SavedStop>,
	monitor: MonitoringStops,
	size: WidgetSize,
	modifier: GlanceModifier = GlanceModifier,
) {
	items(savedStops.mapNotNull {
		STOPS[it.line.line]?.get(it.stop)?.let { s -> Pair(it.line, s) }
	}, itemId = { it.second.id.toLong() }) { (line, stop) ->
		val l = lines[line]!!.second
		StopMonitoring(
			ctx,
			darkMode,
			line.kind,
			l,
			stop,
			size,
			monitor.map[stop.zda.toString()],
			modifier.padding(top = 12.dp)
		)
	}
}

@Composable
fun StopMonitoring(
	ctx: Context,
	darkMode: Boolean,
	kind: LineKind,
	line: LineState,
	stop: Stop,
	size: WidgetSize,
	monitor: List<MonitoringStop>?,
	modifier: GlanceModifier = GlanceModifier
) {
	val wrap: @Composable (@Composable () -> Unit) -> Unit = when (size) {
		WidgetSize.SMALL -> { it ->
			Column(
				modifier.fillMaxWidth(),
				horizontalAlignment = Alignment.CenterHorizontally
			) { it() }
		}

		else -> { it -> Row(modifier, verticalAlignment = Alignment.Top) { it() } }
	}
	val (stopHeadMod, stopMod) = when (size) {
		WidgetSize.SMALL -> GlanceModifier.fillMaxWidth().padding(bottom = 8.dp) to GlanceModifier
		WidgetSize.MEDIUM -> GlanceModifier.width(96.dp) to GlanceModifier.padding(start = 8.dp)
		WidgetSize.LARGE -> GlanceModifier.width(128.dp) to GlanceModifier.padding(start = 8.dp)
	}
	wrap {
		val textStyle = TextStyle(GlanceTheme.colors.onSurface)
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			modifier = stopHeadMod
		) {
			Line(darkMode, kind, line, GlanceTheme.colors.surface)
			Text(text = stop.name, style = textStyle, maxLines = 1)
		}
		Column(stopMod) {
			val monitor = monitor?.let { convertMonitor(stop, line.line, it) }
			if (monitor.isNullOrEmpty()) {
				Text(ctx.getString(R.string.home_train_nothing), style = textStyle)
			} else {
				monitor.forEach { (destination, monitor) ->
					Text(
						text = destination,
						style = textStyle.copy(fontWeight = FontWeight.Bold),
						maxLines = 1
					)
					Row(GlanceModifier.padding(bottom = 16.dp).fillMaxWidth()) {
						monitor.subList(0, min(monitor.size, 4)).forEach {
							displayStop(ctx, it).let { (v, err) ->
								Text(
									text = v,
									style = TextStyle(
										color = if (err) GlanceTheme.colors.error
										else GlanceTheme.colors.onSurface
									),
									modifier = GlanceModifier.padding(top = 4.dp)
										.padding(end = 8.dp),
									maxLines = 1,
								)
							}
						}
					}
				}
			}
		}
	}
}