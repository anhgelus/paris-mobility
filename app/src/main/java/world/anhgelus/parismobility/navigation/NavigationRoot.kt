package world.anhgelus.parismobility.navigation

import androidx.compose.animation.core.EaseInOut
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import world.anhgelus.parismobility.screens.HomeScreen
import world.anhgelus.parismobility.screens.NetworkScreen
import kotlin.math.pow

@Preview(showBackground = true)
@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier
) {
    val rootBackStack = rememberNavBackStack(Route.Home)
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
                val delay = 100
                val scale = 0.95f
                val duration = 300
                val easing = EaseInOut
                scaleIn(
                    initialScale = scale.pow(0.5f),
                    animationSpec = tween(
                        delayMillis = delay,
                        durationMillis = duration,
                        easing = easing
                    )
                ) +
                        fadeIn(
                            animationSpec = tween(
                                delayMillis = delay,
                                durationMillis = 2 * duration / 3,
                                easing = easing
                            )
                        ) togetherWith
                        scaleOut(
                            animationSpec = tween(durationMillis = duration, easing = easing),
                            targetScale = scale
                        ) +
                        fadeOut(animationSpec = tween(durationMillis = duration, easing = easing))
            },
            entryProvider = entryProvider {
                entry<Route.Home> {
                    HomeScreen()
                }
                entry<Route.Network> {
                    NetworkScreen()
                }
            },
        )
    }
}