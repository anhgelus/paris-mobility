package world.anhgelus.parismobility.widget

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
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
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
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
import world.anhgelus.parismobility.ui.theme.ParisMobiliteTheme
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
		val darkMode = GlanceTheme.colors.surface.getColor(ctx).luminance() < 0.5
		ParisMobiliteTheme(darkMode, glance = true) {
			val titleStyle = TextStyle(
				fontSize = 20.sp,
				color = GlanceTheme.colors.onSurface,
				fontFamily = FontFamily("sans-serif"),
			)
			val pref = PreferencesRepository(ctx)
			val savedLines by pref.linesFlow.collectAsState(emptySet())
			val savedStops by pref.stopsFlow.collectAsState(emptySet())
			val monitor = MonitoringStops(
				repo?.stops
					?.collectAsState(emptyMap())
					?.value
					?: emptyMap()
			)
			val lines = repo?.lines?.collectAsState()?.value ?: LinesRepository.loadLines()
			val lastSync = repo?.lastSync?.collectAsState()?.value
			Scaffold(
				titleBar = {
					TitleBar(
						startIcon = ImageProvider(R.drawable.ic_launcher_foreground),
						title = ctx.getString(R.string.app_name),
						iconColor = GlanceTheme.colors.onSecondaryContainer,
						textColor = GlanceTheme.colors.onSecondaryContainer,
						modifier = GlanceModifier.background(GlanceTheme.colors.secondaryContainer),
					) {
						lastSync?.let {
							Text(
								text = it.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
								style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer)
							)
						}
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
				horizontalPadding = 0.dp,
			) {
				val connected = repo?.isConnected?.collectAsState()?.value ?: false
				Column {
					if (!connected && lastSync != null) {
						Row(
							modifier = GlanceModifier
								.fillMaxWidth()
								.background(GlanceTheme.colors.errorContainer)
								.padding(8.dp),
							verticalAlignment = Alignment.CenterVertically,
						) {
							Image(
								provider = ImageProvider(R.drawable.outline_signal_cellular_connected_no_internet_0_bar_24),
								contentDescription = null,
								colorFilter = ColorFilter.tint(GlanceTheme.colors.onErrorContainer),
								modifier = GlanceModifier.padding(end = 8.dp)
							)
							Text(
								text = ctx.getString(R.string.disconnected),
								style = TextStyle(color = GlanceTheme.colors.onErrorContainer),
							)
						}
					}
					LazyColumn {
						val modifier = GlanceModifier.padding(horizontal = 16.dp)
						item {
							Text(
								text = ctx.getString(R.string.home_disruptions),
								style = titleStyle,
								modifier = modifier.padding(top = 16.dp)
							)
						}
						item {
							FlowRow(
								savedLines.mapNotNull {
									lines[it.kind]?.get(it.line)?.let { v -> Pair(it.kind, v) }
								},
								padding = 16.dp,
								modifier = GlanceModifier.padding(top = 8.dp),
							) { (kind, line), modifier ->
								Line(darkMode, kind, line, GlanceTheme.colors.surface, modifier)
							}
						}
						item {
							Text(
								text = ctx.getString(R.string.home_next_trains),
								style = titleStyle,
								modifier = modifier.padding(top = 16.dp)
							)
						}
						stopsMonitoring(
							ctx, darkMode, lines, savedStops, monitor,
							modifier.padding(bottom = 16.dp),
						)
					}
				}
			}
		}
	}
}