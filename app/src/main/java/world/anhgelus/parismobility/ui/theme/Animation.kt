package world.anhgelus.parismobility.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.scene.Scene

fun <T : Any> transitionSub(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform {
    return { slideInHorizontally { it } togetherWith slideOutHorizontally { -it } }
}

fun <T : Any> transitionSubPop(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform {
    return { slideInHorizontally { -it } togetherWith slideOutHorizontally { it } }
}

fun <T : Any> transitionSubPredictivePop(): AnimatedContentTransitionScope<Scene<T>>.(Int) -> ContentTransform {
    return { transitionSubPop<T>()() }
}