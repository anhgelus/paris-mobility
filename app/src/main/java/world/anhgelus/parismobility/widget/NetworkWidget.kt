package world.anhgelus.parismobility.widget

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.padding
import androidx.glance.text.FontFamily
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import world.anhgelus.parismobility.R
import world.anhgelus.parismobility.data.LinesRepository
import world.anhgelus.parismobility.data.MonitoringStops
import world.anhgelus.parismobility.data.PreferencesRepository
import world.anhgelus.parismobility.data.backend.BackendDataSource
import world.anhgelus.parismobility.data.backend.Connection
import world.anhgelus.parismobility.data.backend.OneShotConnection
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class NetworkWidget : GlanceAppWidget() {
	private val connectionState = MutableStateFlow<Connection?>(null)

	class WidgetReceiver : GlanceAppWidgetReceiver() {
		override val glanceAppWidget: GlanceAppWidget = NetworkWidget()
	}

	override val sizeMode: SizeMode = SizeMode.Exact

	override suspend fun provideGlance(
		context: Context,
		id: GlanceId
	) {
		provideContent {
			val connection = connectionState.collectAsState(null).value
				?: OneShotConnection(
					context.getSystemService(ConnectivityManager::class.java)!!
				).also { conn ->
					connectionState.update { conn }
				}
			val pref = PreferencesRepository(context)
			val savedStops by pref.stopsFlow.collectAsState(emptySet())
			Content(
				context,
				WidgetRepository(BackendDataSource(connection), savedStops),
				onSync = {
					//TODO: use a service
					CoroutineScope(Dispatchers.IO).launch {
						connection.close()
						connectionState.update {
							OneShotConnection(
								context.getSystemService(ConnectivityManager::class.java)!!
							)
						}
						update(context, id)
					}
				})
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
			Content(context, onSync = {})
		}
	}

	@Composable
	fun Content(ctx: Context, repo: WidgetRepository? = null, onSync: () -> Unit) {
		GlanceTheme {
			val titleStyle = TextStyle(
				fontSize = 20.sp,
				color = GlanceTheme.colors.onSurface,
				fontFamily = FontFamily("sans-serif"),
			)
			val pref = PreferencesRepository(ctx)
			val savedLines by pref.linesFlow.collectAsState(emptySet())
			val savedStops by pref.stopsFlow.collectAsState(emptySet())
			val monitor = repo?.stops
				?.collectAsState(MonitoringStops(emptyMap()))
				?.value
				?: MonitoringStops(emptyMap())
			val lines = repo?.lines?.collectAsState()?.value ?: LinesRepository.loadLines()
			val lastSync = repo?.lastSync?.collectAsState()?.value ?: LocalTime.now()
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
							ImageProvider(R.drawable.outline_sync_24),
							"Recharger",
							backgroundColor = GlanceTheme.colors.secondaryContainer,
							contentColor = GlanceTheme.colors.primary,
							onClick = onSync,
						)
					}
				},
				backgroundColor = GlanceTheme.colors.surface,
			) {
				LazyColumn(modifier = GlanceModifier.padding(top = 16.dp)) {
					item {
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
					}
					item {
						Column(modifier = GlanceModifier.padding(top = 16.dp)) {
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
	}
}