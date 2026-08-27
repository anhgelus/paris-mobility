package world.anhgelus.parismobility.navigation

import android.content.Context
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import world.anhgelus.parismobility.models.GeneralViewModel
import world.anhgelus.parismobility.models.HomeViewModel
import world.anhgelus.parismobility.models.NetworkViewModel
import world.anhgelus.parismobility.screens.HomeScreen
import world.anhgelus.parismobility.screens.MapScreen
import world.anhgelus.parismobility.screens.NetworkScreen
import kotlin.math.pow

@Composable
fun NavigationRoot(
	ctx: Context,
	model: GeneralViewModel,
	backStack: NavBackStack<NavKey>,
) {
	val homeViewModel = viewModel {
		HomeViewModel(
			model.preferencesRepository,
			model.linesRepository,
		)
	}
	val networkViewModel = viewModel { NetworkViewModel(model.linesRepository) }
	NavDisplay(
		modifier = Modifier.fillMaxSize(),
		backStack = backStack,
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
		predictivePopTransitionSpec = {
			slideInHorizontally {
				-it
			} + fadeIn(
				initialAlpha = 0.5f,
				animationSpec = tween(durationMillis = 200, easing = EaseInCubic)
			) togetherWith slideOutHorizontally {
				it
			} + fadeOut(
				targetAlpha = 0.9f,
				animationSpec = tween(durationMillis = 200, easing = EaseInCubic)
			)
		},
		entryProvider = entryProvider {
			val modif = Modifier.padding(horizontal = 16.dp)
			entry<Route.Home> {
				HomeScreen(ctx, homeViewModel, modif)
			}
			entry<Route.Network> {
				NetworkScreen(networkViewModel, modif)
			}
			entry<Route.Map> {
				MapScreen(modif)
			}
		},
	)
}