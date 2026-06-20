package world.anhgelus.parismobility.navigation

import android.content.Context
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
import world.anhgelus.parismobility.data.LinesRepository
import world.anhgelus.parismobility.data.PreferencesRepository
import world.anhgelus.parismobility.models.HomeViewModel
import world.anhgelus.parismobility.models.NetworkViewModel
import world.anhgelus.parismobility.screens.HomeScreen
import world.anhgelus.parismobility.screens.NetworkScreen
import kotlin.math.pow

@Composable
fun NavigationRoot(
    ctx: Context,
    preferencesRepo: PreferencesRepository,
    linesRepo: LinesRepository,
    modifier: Modifier = Modifier
) {
    val rootBackStack = rememberNavBackStack(Route.Home)

    val homeViewModel = viewModel {
        HomeViewModel(
            preferencesRepo, linesRepo, listOf(),
        )
    }
    val networkViewModel = viewModel { NetworkViewModel(linesRepo) }
    // disable return function for main nav
    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar(selectedKey = rootBackStack.last()) { rootBackStack.add(it) }
        }
    ) { innerPadding ->
        NavDisplay(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            backStack = rootBackStack,
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
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
                val modif = Modifier.padding(horizontal = 16.dp)
                entry<Route.Home> {
                    HomeScreen(ctx, homeViewModel, modif)
                }
                entry<Route.Network> {
                    NetworkScreen(networkViewModel, modif)
                }
            },
        )
    }
}