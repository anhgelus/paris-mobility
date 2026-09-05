package world.anhgelus.parismobility.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.unit.ColorProvider
import world.anhgelus.parismobility.LineDisruptionActivity
import world.anhgelus.parismobility.data.Line
import world.anhgelus.parismobility.data.LineState
import world.anhgelus.parismobility.data.Severity
import world.anhgelus.parismobility.models.LineKind

@Composable
fun <T> FlowRow(
	items: Collection<T>,
	padding: Dp,
	modifier: GlanceModifier = GlanceModifier,
	render: @Composable ((T, GlanceModifier) -> Unit)
) {
	val w = (LocalSize.current.width - padding * 2).value
	val n = minOf((w / 56).toInt(), 5)
	val l = items.toList()
	val padLeft = (w - n * 56) / 2f
	Column(modifier = modifier.padding(horizontal = padding).fillMaxWidth()) {
		l.chunked(n).forEachIndexed { i, sub ->
			Row(modifier = GlanceModifier.fillMaxWidth()) {
				Spacer(GlanceModifier.width(padLeft.dp))
				sub.forEach { line ->
					render(line, GlanceModifier.padding(horizontal = 4.dp))
				}
			}
			if (i < sub.size - 1) Spacer(GlanceModifier.height(8.dp))
		}
	}
}

@Composable
fun Line(
	dark: Boolean,
	kind: LineKind,
	line: LineState,
	backgroundColor: ColorProvider,
	modifier: GlanceModifier = GlanceModifier,
) {
	val sev by line.disruptionSeverity
	val modifier = modifier.clickable(
		actionStartActivity<LineDisruptionActivity>(
			actionParametersOf(
				ActionParameters.Key<Byte>(LineDisruptionActivity.KIND_KEY) to kind.ordinal.toByte(),
				ActionParameters.Key<String>(LineDisruptionActivity.LINE_KEY) to line.line.id,
			)
		)
	)
	val wrap: @Composable (@Composable () -> Unit) -> Unit = if (sev != Severity.INFORMATION) {
		{ BorderBox(2.dp, sev.color.first, backgroundColor, modifier, it) }
	} else {
		{ Box(content = it, modifier = modifier) }
	}
	wrap {
		LineImage(
			dark = dark,
			kind = kind,
			line = line.line,
			modifier = if (sev == Severity.INFORMATION) GlanceModifier.padding(4.dp).size(48.dp)
			else GlanceModifier.size(32.dp),
		)
	}
}

@Composable
fun LineImage(
	dark: Boolean,
	kind: LineKind,
	line: Line,
	modifier: GlanceModifier = GlanceModifier,
	forceBackground: Boolean = false
) {
	Image(
		provider = ImageProvider(line.icon),
		contentDescription = line.name,
		modifier = modifier
			.background(
				color =
					if ((dark || forceBackground) && kind.requiresBackground) Color.White
					else Color.Transparent,
			),
	)
}

@Composable
fun BorderBox(
	width: Dp,
	color: Color,
	backgroundColor: ColorProvider,
	modifier: GlanceModifier = GlanceModifier,
	content: @Composable () -> Unit
) {
	Box(modifier = modifier) {
		Box(
			modifier = GlanceModifier
				.background(color)
				.cornerRadius(12.dp)
				.padding(width)
		) {
			Box(
				modifier = GlanceModifier
					.cornerRadius(10.dp)
					.background(backgroundColor)
					.padding(6.dp),
			) { content() }
		}
	}
}