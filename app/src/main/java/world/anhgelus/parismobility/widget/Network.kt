package world.anhgelus.parismobility.widget

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.padding
import androidx.glance.text.FontFamily
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import world.anhgelus.parismobility.R
import world.anhgelus.parismobility.data.LinesRepository
import world.anhgelus.parismobility.data.MonitoringStops
import world.anhgelus.parismobility.data.PreferencesRepository
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class Network : GlanceAppWidget() {
	class WidgetReceiver : GlanceAppWidgetReceiver() {
		override val glanceAppWidget: GlanceAppWidget = Network()
	}

	override val sizeMode: SizeMode = SizeMode.Exact

	override suspend fun provideGlance(
		context: Context,
		id: GlanceId
	) {
		provideContent {
			val repo = remember { LinesRepository.getInstance() }
			Content(context, repo)
		}
	}

	@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
	override suspend fun providePreview(
		context: Context,
		widgetCategory: Int
	) {
		GlanceAppWidgetManager(context).setWidgetPreviews(WidgetReceiver::class).let {
			if (it != GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS) return
		}
		provideContent {
			Content(context)
		}
	}

	@Composable
	fun Content(ctx: Context, linesRepo: LinesRepository? = null) {
		GlanceTheme {
			val titleStyle = TextStyle(
				fontSize = 20.sp,
				color = GlanceTheme.colors.onSurface,
				fontFamily = FontFamily("sans-serif"),
			)
			val pref = PreferencesRepository(ctx)
			val savedLines by pref.linesFlow.collectAsState(emptySet())
			val savedStops by pref.stopsFlow.collectAsState(emptySet())
			val monitor = linesRepo?.monitorStops
				?.collectAsState(MonitoringStops(emptyMap()))
				?.value
				?: MonitoringStops(emptyMap())
			val lines = linesRepo?.lines?.collectAsState()?.value ?: LinesRepository.loadLines()
			val lastSync = linesRepo?.lastSync?.collectAsState()?.value ?: LocalTime.now()
			Scaffold(
				titleBar = {
					TitleBar(
						startIcon = ImageProvider(R.drawable.ic_launcher_foreground),
						title = "Paris Mobilité",
						iconColor = GlanceTheme.colors.onSecondaryContainer,
						textColor = GlanceTheme.colors.onSecondaryContainer,
						modifier = GlanceModifier.background(GlanceTheme.colors.secondaryContainer),
					) {
						Text(
							text = lastSync
								.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
							style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer)
						)
						CircleIconButton(
							ImageProvider(R.drawable.ic_launcher_foreground),
							"Recharger",
							backgroundColor = GlanceTheme.colors.secondaryContainer,
							contentColor = GlanceTheme.colors.primary,
							onClick = {},
						)
					}
				},
				backgroundColor = GlanceTheme.colors.surface,
			) {
				Column(modifier = GlanceModifier.padding(top = 16.dp)) {
					Column {
						Text(text = "État de vos lignes", style = titleStyle)
						FlowRow(
							savedLines.mapNotNull {
								lines[it.kind]?.get(it.line)?.let { v -> Pair(it.kind, v) }
							},
							modifier = GlanceModifier.padding(top = 8.dp)
						) { (kind, line) ->
							Line(ctx, kind, line, GlanceTheme.colors.surface)
						}
					}
					Text(
						text = "Prochains passages",
						style = titleStyle,
						modifier = GlanceModifier.padding(top = 16.dp),
					)
				}
			}
		}
	}
}