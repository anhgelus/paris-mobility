package world.anhgelus.parismobility.widget

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getSystemService
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
import world.anhgelus.parismobility.data.LinesDataSource
import world.anhgelus.parismobility.data.MonitoringStops
import world.anhgelus.parismobility.data.SavedLine
import world.anhgelus.parismobility.data.SavedStop
import world.anhgelus.parismobility.data.backend.BackendConnection
import world.anhgelus.parismobility.data.backend.BackendDataSource
import world.anhgelus.parismobility.models.GeneralViewModel
import world.anhgelus.parismobility.models.HomeViewModel

class Network : GlanceAppWidget() {
	class WidgetReceiver : GlanceAppWidgetReceiver() {
		override val glanceAppWidget: GlanceAppWidget = Network()
	}

	override val sizeMode: SizeMode = SizeMode.Exact

	override suspend fun provideGlance(
		context: Context,
		id: GlanceId
	) {
		val conn = BackendConnection(getSystemService(context, ConnectivityManager::class.java)!!)
		provideContent {
			val model = remember {
				GeneralViewModel(
					context,
					LinesDataSource,
					BackendDataSource(conn)
				)
			}
			val homeViewModel = remember {
				HomeViewModel(model.preferencesRepository, model.linesRepository)
			}
			Content(context, homeViewModel)
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
	fun Content(ctx: Context, model: HomeViewModel? = null) {
		GlanceTheme {
			val titleStyle = TextStyle(
				fontSize = 20.sp,
				color = GlanceTheme.colors.onSurface,
				fontFamily = FontFamily("sans-serif"),
			)
			val content = getContent(model)
			val lines by model!!.lines.collectAsState()
			Scaffold(
				titleBar = {
					TitleBar(
						startIcon = ImageProvider(R.drawable.ic_launcher_foreground),
						title = "Paris Mobilité",
						iconColor = GlanceTheme.colors.onSecondaryContainer,
						textColor = GlanceTheme.colors.onSecondaryContainer,
						modifier = GlanceModifier.background(GlanceTheme.colors.secondaryContainer),
					) {
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
							content.first.mapNotNull {
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

	@Composable
	private fun getContent(model: HomeViewModel?): Triple<Set<SavedLine>, Set<SavedStop>, MonitoringStops> {
		model?.let { model ->
			val lines by model.savedLines.collectAsState()
			val stops by model.savedStops.collectAsState()
			val monitor by model.monitoringStops.collectAsState()
			return Triple(lines, stops, monitor)
		}
		return Triple(emptySet(), emptySet(), MonitoringStops(emptyMap()))
	}
}