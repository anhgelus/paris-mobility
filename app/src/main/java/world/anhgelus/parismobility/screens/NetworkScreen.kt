package world.anhgelus.parismobility.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import world.anhgelus.parismobility.data.Disruption
import world.anhgelus.parismobility.data.Disruptions
import world.anhgelus.parismobility.data.Line
import world.anhgelus.parismobility.data.LineGroups
import world.anhgelus.parismobility.data.NetworkError
import world.anhgelus.parismobility.data.Result
import world.anhgelus.parismobility.models.NetworkViewModel
import world.anhgelus.parismobility.navigation.Route
import world.anhgelus.parismobility.ui.DisruptionCard
import world.anhgelus.parismobility.ui.LineImage
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
    val disruptions by viewModel.disruptions.collectAsStateWithLifecycle(Result.Error(NetworkError.NOT_CONNECTED))
    var dis: Disruptions? = null
    var error: NetworkError? = null
    var message: String? = null
    disruptions.onSuccess {
        dis = it
    }.onError { kind, msg ->
        error = kind
        message = msg
    }
    if (error != null) {
        Text(
            text = error.displayError + if (message != null) ": $message" else "",
            color = MaterialTheme.colorScheme.onError,
            modifier = modifier.background(color = MaterialTheme.colorScheme.error),
        )
        return
    }
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
                LineScreen(kind, line, dis!![line.id], modifier)
            }
        },
    )
}

@Composable
fun GeneralScreen(
    groups: LineGroups,
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
        items(items = groups.filter { (key, _) -> key != LK.BUS }.toList()) { (kind, lines) ->
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
    Column(
        modifier = Modifier.background(line.color),
    ) {
        val network = when (kind) {
            LK.RER -> "RER"
            LK.METRO -> "Métro"
            LK.TRAM -> "Tram"
            LK.TRANSILIEN -> "Ligne"
            else -> line.network!!
        }
        val title = "$network ${line.name}"
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color(
                        ColorUtils.blendARGB(
                            line.color.toArgb(),
                            Color.Black.toArgb(),
                            0.3f
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Image(
                painter = painterResource(kind.logoId!!),
                contentDescription = "Logo du ${kind.displayName}",
                modifier = Modifier.size(64.dp)
            )
            LineImage(kind, line, Modifier.size(64.dp), true)
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.fillMaxHeight(),
        ) {
            item {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(128.dp)
                        .background(Color.White)
                        .innerShadow(
                            RectangleShape,
                            Shadow(
                                radius = 4.dp,
                                spread = 1.dp,
                                color = Color.Black,
                            )
                        )
                )
            }
            disruptions?.let { dis ->
                items(minOf(3, dis.size), key = { dis[it].id }) { i ->
                    val it = dis[i]
                    DisruptionCard(
                        it.copy(
                            title = it.title.removePrefix("$title : ").removePrefix("$title - ")
                        ),
                        modifier
                    )
                }
                item {
                    if (dis.size >= 3) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalButton(onClick = {}) {
                                Text(text = "Voir plus", style = Typography.bodyLarge)
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.padding(bottom = 16.dp)) }
        }
    }
}