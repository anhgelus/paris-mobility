package world.anhgelus.parismobility.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.flow.StateFlow
import world.anhgelus.parismobility.data.LineGroups
import world.anhgelus.parismobility.data.MonitoringStops
import world.anhgelus.parismobility.data.SavedLine
import world.anhgelus.parismobility.data.SavedStop
import world.anhgelus.parismobility.data.get
import world.anhgelus.parismobility.models.HomeViewModel
import world.anhgelus.parismobility.navigation.Route
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

@Composable
fun GeneralScreen(
	groups: LineGroups,
	savedLines: Collection<SavedLine>,
	savedStops: Collection<SavedStop>,
	monitoredStops: StateFlow<MonitoringStops>,
	modifier: Modifier = Modifier,
) {
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
			LinesRow { size ->
				val lines = savedLines.mapNotNull { groups[it] }
				lines.forEach { (kind, line) ->
					L(kind, line, onClick = {}, modifier = Modifier.size(size))
				}
				lines.size
			}
		}
		StopsMonitoring(groups, savedStops, monitoredStops)
		Spacer(modifier = Modifier.padding(bottom = 16.dp))
	}
}