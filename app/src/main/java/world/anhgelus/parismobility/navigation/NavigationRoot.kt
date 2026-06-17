package world.anhgelus.parismobility.navigation

import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import world.anhgelus.parismobility.R
import world.anhgelus.parismobility.models.HomeViewModel
import world.anhgelus.parismobility.models.LineDataState
import world.anhgelus.parismobility.models.NetworkViewModel
import world.anhgelus.parismobility.models.StopDataState
import world.anhgelus.parismobility.models.TrafficQuality
import world.anhgelus.parismobility.screens.HomeScreen
import world.anhgelus.parismobility.screens.NetworkScreen
import kotlin.math.pow

@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier
) {
    val rootBackStack = rememberNavBackStack(Route.Home)
    val lineA = LineDataState(
        "A",
        R.drawable.ic_launcher_foreground,
        TrafficQuality.MAJOR_ISSUE
    )
    val line7 = LineDataState("7", R.drawable.ic_launcher_foreground)
    val lineJ = LineDataState(
        "J",
        R.drawable.ic_launcher_foreground,
        TrafficQuality.MINOR_ISSUE
    )

    val stopA = StopDataState(
        "Rueil-Malmaison", lineA, listOf(
            "Marne-la-Vallée" to listOf("3 min", "12 min"),
            "Boissy-Saint-Léger" to listOf("8 min"),
        )
    )
    val stop7 = StopDataState(
        "Jussieu", line7, listOf(
            "La Courneuve" to listOf("2 min", "7 min", "12 min")
        )
    )
    val stopJ = StopDataState(
        "Gare Saint-Lazare", lineJ, listOf(
            "Gisors" to listOf("18 min"),
            "Ermont-Aubonne" to listOf("8 min", "26 min"),
            "Pontoise" to listOf("1 min", "48 min"),
            "Mantes-la-Jolie" to listOf("12 min", "33 min")
        )
    )

    val homeViewModel = viewModel {
        HomeViewModel(
            listOf(lineA, line7, lineJ),
            listOf(stopA, stop7, stopJ)
        )
    }
    val networkViewModel = viewModel {
        NetworkViewModel(
            listOf(
                "RER" to listOf(
                    lineA,
                    LineDataState("B", R.drawable.ic_launcher_foreground),
                    LineDataState("C", R.drawable.ic_launcher_foreground),
                    LineDataState("D", R.drawable.ic_launcher_foreground),
                    LineDataState("E", R.drawable.ic_launcher_foreground),
                ),
                "Métro" to listOf(
                    LineDataState("1", R.drawable.ic_launcher_foreground),
                    LineDataState("2", R.drawable.ic_launcher_foreground),
                    LineDataState("3", R.drawable.ic_launcher_foreground),
                    LineDataState("4", R.drawable.ic_launcher_foreground),
                    LineDataState("5", R.drawable.ic_launcher_foreground),
                    LineDataState("6", R.drawable.ic_launcher_foreground),
                    line7,
                    LineDataState("8", R.drawable.ic_launcher_foreground),
                    LineDataState("9", R.drawable.ic_launcher_foreground),
                    LineDataState("10", R.drawable.ic_launcher_foreground),
                    LineDataState("11", R.drawable.ic_launcher_foreground),
                    LineDataState("12", R.drawable.ic_launcher_foreground),
                    LineDataState("13", R.drawable.ic_launcher_foreground),
                    LineDataState("14", R.drawable.ic_launcher_foreground),
                )
            )
        )
    }
    // disable return function for main nav
    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar(selectedKey = rootBackStack.last()) {
                rootBackStack.removeAt(rootBackStack.lastIndex)
                rootBackStack.add(it)
            }
        }
    ) { innerPadding ->
        NavDisplay(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            backStack = rootBackStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator()
            ),
            transitionSpec = {
                val scale = 0.95f
                val duration = 200
                val delay = duration / 3
                val easing = EaseInCubic
                scaleIn(
                    initialScale = scale.pow(0.5f),
                    animationSpec = tween(
                        delayMillis = delay,
                        durationMillis = duration,
                        easing = easing
                    )
                ) + fadeIn(
                    animationSpec = tween(
                        delayMillis = delay,
                        durationMillis = 2 * duration / 3,
                        easing = easing
                    )
                ) togetherWith scaleOut(
                    animationSpec = tween(durationMillis = duration, easing = easing),
                    targetScale = scale
                ) + fadeOut(animationSpec = tween(durationMillis = duration, easing = easing))
            },
            entryProvider = entryProvider {
                entry<Route.Home> {
                    HomeScreen(homeViewModel)
                }
                entry<Route.Network> {
                    NetworkScreen(networkViewModel)
                }
            },
        )
    }
}