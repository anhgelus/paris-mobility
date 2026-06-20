package world.anhgelus.parismobility.screens

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import world.anhgelus.parismobility.models.LineKind as LK

@Composable
fun NetworkScreen(
    viewModel: NetworkViewModel,
    modifier: Modifier = Modifier,
) {
    val networkBackStack = rememberNavBackStack(Route.Network)

    NavDisplay(
        backStack = networkBackStack,
        transitionSpec = { slideInHorizontally { it } togetherWith slideOutHorizontally { -it } },
        popTransitionSpec = { slideInHorizontally { -it } togetherWith slideOutHorizontally { it } },
        predictivePopTransitionSpec = { slideInHorizontally { -it } togetherWith slideOutHorizontally { it } },
        entryProvider = entryProvider {
            entry<Route.Network> {
                val groups by viewModel.lines.collectAsStateWithLifecycle()
                GeneralScreen(groups, onClick = { kind, line ->
                    networkBackStack.add(Route.Network.SpecificLine(kind, line))
                }, modifier)
            }
            entry<Route.Network.SpecificLine> { (kind, line) ->
                val disruptions by viewModel.disruptions.collectAsStateWithLifecycle()
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
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        ScreenTitle("Réseau")
        groups.filter { it.key != LK.BUS }.forEach { (kind, lines) ->
            LineKind(
                modifier = Modifier.padding(horizontal = 16.dp),
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
        modifier = Modifier.background(line.color)
    ) {
        Column(
            modifier = modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            val network = when (kind) {
                LK.RER -> "RER"
                LK.METRO -> "Métro"
                LK.TRAM -> "Tram"
                LK.TRANSILIEN -> "Ligne"
                else -> line.network!!
            }
            val title = "$network ${line.name}"
            ScreenTitle(content = title, color = line.textColor)
            var i = 0
            disruptions?.takeWhile {
                DisruptionCard(
                    it.copy(
                        title = it.title.removePrefix("$title : ").removePrefix("$title - ")
                    )
                )
                ++i < 3
            }
            disruptions?.size?.let {
                if (it >= 3) {
                    Button(onClick = {}, modifier = modifier) {
                        Text("Voir plus")
                    }
                }
            }
        }
    }
}