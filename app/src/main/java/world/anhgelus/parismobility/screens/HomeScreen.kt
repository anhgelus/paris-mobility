package world.anhgelus.parismobility.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import world.anhgelus.parismobility.data.Line
import world.anhgelus.parismobility.models.HomeViewModel
import world.anhgelus.parismobility.models.StopDataState
import world.anhgelus.parismobility.navigation.Route
import world.anhgelus.parismobility.ui.LineDetailed
import world.anhgelus.parismobility.ui.LineKind
import world.anhgelus.parismobility.ui.ScreenTitle
import world.anhgelus.parismobility.ui.SectionTitle
import world.anhgelus.parismobility.ui.StopsMonitoring
import world.anhgelus.parismobility.ui.theme.Typography
import world.anhgelus.parismobility.ui.theme.transitionSub
import world.anhgelus.parismobility.ui.theme.transitionSubPop
import world.anhgelus.parismobility.ui.theme.transitionSubPredictivePop
import world.anhgelus.parismobility.models.LineKind as LK

@Composable
fun HomeScreen(
    ctx: Context,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val homeBackStack = rememberNavBackStack(Route.Home)

    val lines by viewModel.lines.collectAsStateWithLifecycle()
    val savedLines = viewModel.getSavedLines()
    val stops by viewModel.stops.collectAsStateWithLifecycle()

    NavDisplay(
        backStack = homeBackStack,
        transitionSpec = transitionSub(),
        popTransitionSpec = transitionSubPop(),
        predictivePopTransitionSpec = transitionSubPredictivePop(),
        entryProvider = entryProvider {
            entry<Route.Home> {
                GeneralScreen(
                    savedLines,
                    stops,
                    modifier
                ) { homeBackStack.add(Route.Home.Modify) }
            }
            entry<Route.Home.Modify> {
                ModifyScreen(lines, savedLines) { kind, line, added ->
                    if (added) viewModel.saveLine(ctx, kind, line)
                    else viewModel.removeLine(ctx, kind, line)
                }
            }
        }
    )
}

@Composable
fun GeneralScreen(
    lines: Collection<Line>,
    stops: List<StopDataState>,
    modifier: Modifier = Modifier,
    onModifyClick: () -> Unit,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        ScreenTitle("Votre réseau")
        LineKind(
            modifier = Modifier.padding(horizontal = 16.dp),
            name = "État de vos lignes",
            lines = lines,
            kind = LK.RER,
            onClick = { _, _ -> },
        )
        StopsMonitoring(stops)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            FilledTonalButton(onClick = onModifyClick) {
                Text(text = "Modifier", style = Typography.bodyLarge)
            }
        }
    }
}

@Composable
fun ModifyScreen(
    groups: Map<LK, List<Line>>,
    savedLines: Collection<Line>,
    onUpdate: (LK, Line, Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        ScreenTitle("Lignes sauvegardées")
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            groups.filter { it.key != LK.BUS }.forEach { (kind, lines) ->
                item {
                    SectionTitle(
                        content = kind.displayName,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                items(lines) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = { onUpdate(kind, it, !savedLines.contains(it)) }),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        LineDetailed(kind, it)
                        Checkbox(
                            checked = savedLines.contains(it),
                            onCheckedChange = { change -> onUpdate(kind, it, change) }
                        )
                    }
                }
            }
        }
    }
}