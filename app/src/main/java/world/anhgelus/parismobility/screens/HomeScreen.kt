package world.anhgelus.parismobility.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.flow.StateFlow
import world.anhgelus.parismobility.data.Disruptions
import world.anhgelus.parismobility.data.Line
import world.anhgelus.parismobility.data.LineGroups
import world.anhgelus.parismobility.data.MonitoringStops
import world.anhgelus.parismobility.data.SavedLine
import world.anhgelus.parismobility.data.SavedStop
import world.anhgelus.parismobility.data.get
import world.anhgelus.parismobility.models.HomeViewModel
import world.anhgelus.parismobility.models.LineKind
import world.anhgelus.parismobility.navigation.Route
import world.anhgelus.parismobility.ui.DisruptionsDrawer
import world.anhgelus.parismobility.ui.LinesRow
import world.anhgelus.parismobility.ui.ScreenTitle
import world.anhgelus.parismobility.ui.SectionTitle
import world.anhgelus.parismobility.ui.StopsMonitoring
import world.anhgelus.parismobility.ui.theme.transitionSub
import world.anhgelus.parismobility.ui.theme.transitionSubPop
import world.anhgelus.parismobility.ui.theme.transitionSubPredictivePop
import world.anhgelus.parismobility.ui.Line as L

@Composable
fun HomeScreen(
	ctx: Context,
	viewModel: HomeViewModel,
	disruptions: Disruptions,
	modifier: Modifier = Modifier,
	homeBackStack: NavBackStack<NavKey>,
) {
	val lines by viewModel.lines.collectAsStateWithLifecycle()
	val savedLines by viewModel.savedLines.collectAsStateWithLifecycle()
	val savedStops by viewModel.savedStops.collectAsStateWithLifecycle()

	NavDisplay(
		backStack = homeBackStack,
		transitionSpec = transitionSub(),
		popTransitionSpec = transitionSubPop(),
		predictivePopTransitionSpec = transitionSubPredictivePop(),
		entryProvider = entryProvider {
			entry<Route.Home> {
				GeneralScreen(
					lines,
					savedLines,
					savedStops,
					viewModel.monitoringStops,
					disruptions,
					modifier
				)
			}
			entry<Route.Home.Modify> {
				ModifyScreen(
					groups = lines,
					savedLines = savedLines,
					savedStops = savedStops,
					onUpdateLines = { kind, line, added ->
						if (added) viewModel.saveLine(ctx, kind, line)
						else viewModel.removeLine(ctx, kind, line)
					},
					onUpdateStops = { kind, line, stop, added ->
						if (added) viewModel.saveStops(ctx, kind, line, stop, "")
						else viewModel.removeStops(ctx, kind, line, stop, "")
					},
					onClick = { a, b -> viewModel.changeTab(a, b) },
				)
			}
		}
	)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralScreen(
	groups: LineGroups,
	savedLines: Collection<SavedLine>,
	savedStops: Collection<SavedStop>,
	monitoredStops: StateFlow<MonitoringStops>,
	disruptions: Disruptions,
	modifier: Modifier = Modifier,
) {
	var selected by remember { mutableStateOf<Pair<LineKind, Line>?>(null) }
	Column(
		modifier = modifier.verticalScroll(rememberScrollState()),
		verticalArrangement = Arrangement.spacedBy(32.dp),
	) {
		ScreenTitle("Votre réseau")
		Column(
			modifier = Modifier.padding(horizontal = 16.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp),
		) {
			SectionTitle("État de vos lignes")
			if (savedLines.isEmpty()) {
				Text("Aucune ligne configurée. Cliquez sur l'icon en bas à droite pour en ajouter.")
				return@Column
			}
			LinesRow { size ->
				val lines = savedLines.mapNotNull { groups[it] }
				lines.forEach { (kind, line) ->
					L(
						kind,
						line,
						onClick = { selected = kind to line.line },
						modifier = Modifier.size(size)
					)
				}
				lines.size
			}
		}
		StopsMonitoring(groups, savedStops, monitoredStops, { k, l -> selected = k to l })
		Spacer(modifier = Modifier.padding(bottom = 16.dp))
	}
	selected?.let { (kind, line) ->
		DisruptionsDrawer(kind, line, disruptions, modifier) { selected = null }
	}
}