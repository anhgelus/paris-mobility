package world.anhgelus.parismobility.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import kotlinx.serialization.Serializable
import world.anhgelus.parismobility.R
import world.anhgelus.parismobility.screens.HomeScreen
import world.anhgelus.parismobility.screens.NetworkScreen

@Serializable
sealed interface Route : NavKey {

    @Serializable
    data object Home : Route
    data object Network : Route
}

data class BottomNavItem(
    val painterID: Int,
    val title: String,
)

val TOP_LEVEL_DESTINATIONS = mapOf(
    Route.Home to BottomNavItem(
        painterID = R.drawable.outline_home_24,
        title = "Accueil",
    ),
    Route.Network to BottomNavItem(
        painterID = R.drawable.outline_train_24,
        title = "Réseau",
    )
)

val ROUTES: (NavKey) -> NavEntry<NavKey> = entryProvider {
    entry<Route.Home> {
        HomeScreen()
    }
    entry<Route.Network> {
        NetworkScreen()
    }
}