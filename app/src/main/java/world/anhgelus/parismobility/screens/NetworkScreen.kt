package world.anhgelus.parismobility.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import world.anhgelus.parismobility.data.Disruption
import world.anhgelus.parismobility.data.Line
import world.anhgelus.parismobility.models.NetworkViewModel
import world.anhgelus.parismobility.navigation.Route
import world.anhgelus.parismobility.ui.DisruptionCard
import world.anhgelus.parismobility.ui.LineKind
import world.anhgelus.parismobility.ui.ScreenTitle
import world.anhgelus.parismobility.ui.theme.Typography
import world.anhgelus.parismobility.ui.theme.transitionSub
import world.anhgelus.parismobility.ui.theme.transitionSubPop
import world.anhgelus.parismobility.ui.theme.transitionSubPredictivePop
import world.anhgelus.parismobility.models.LineKind as LK

@Composable
fun NetworkScreen(
    viewModel: NetworkViewModel,
    modifier: Modifier = Modifier,
) {
    val networkBackStack = rememberNavBackStack(Route.Network)

    NavDisplay(
        backStack = networkBackStack,
        transitionSpec = transitionSub(),
        popTransitionSpec = transitionSubPop(),
        predictivePopTransitionSpec = transitionSubPredictivePop(),
        entryProvider = entryProvider {
            entry<Route.Network> {
                val groups by viewModel.lines.collectAsStateWithLifecycle()
                GeneralScreen(groups, onClick = { kind, line ->
                    networkBackStack.add(Route.Network.SpecificLine(kind, line))
                }, modifier)
            }
            entry<Route.Network.SpecificLine> { (kind, line) ->
                val disruptions by viewModel.disruptions.collectAsStateWithLifecycle(emptyMap())
                LineScreen(kind, line, disruptions[line.id], modifier)
            }
        },
    )
}

@Composable
fun GeneralScreen(
    groups: Map<LK, List<Line>>,
    onClick: (LK, Line) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        item {
            ScreenTitle("Réseau")
        }
        items(items = groups.filter { it.key != LK.BUS }.toList()) { (kind, lines) ->
            LineKind(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                name = kind.displayName,
                lines = lines,
                kind = kind,
                onClick = onClick
            )
        }
    }
}

@Composable
fun LineScreen(
    kind: LK,
    line: Line,
    disruptions: List<Disruption>?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = Modifier.background(line.color),
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = modifier.fillMaxHeight(),
        ) {
            val network = when (kind) {
                LK.RER -> "RER"
                LK.METRO -> "Métro"
                LK.TRAM -> "Tram"
                LK.TRANSILIEN -> "Ligne"
                else -> line.network!!
            }
            val title = "$network ${line.name}"
            item {
                ScreenTitle(content = title, color = line.textColor)
            }
            disruptions?.let { dis ->
                items(minOf(3, dis.size), key = { dis[it].id }) { i ->
                    val it = dis[i]
                    DisruptionCard(
                        it.copy(
                            title = it.title.removePrefix("$title : ").removePrefix("$title - ")
                        )
                    )
                }
                if (dis.size >= 3) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            FilledTonalButton(onClick = {}) {
                                Text(text = "Voir plus", style = Typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}