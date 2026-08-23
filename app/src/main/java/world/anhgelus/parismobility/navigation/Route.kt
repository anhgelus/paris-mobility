package world.anhgelus.parismobility.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import world.anhgelus.parismobility.R
import world.anhgelus.parismobility.data.Line
import world.anhgelus.parismobility.models.LineKind

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Home : Route {
        @Serializable
        data object Modify : Route {
            @Serializable
            data object Stops : Route

            @Serializable
            data class Stop(val kind: LineKind, val line: Line) : Route
        }
    }

    @Serializable
    data object Network : Route {
        @Serializable
        data class SpecificLine(val kind: LineKind, val line: Line) : Route
    }

    @Serializable
    data object Map : Route
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
    ),
    Route.Map to BottomNavItem(
        painterID = R.drawable.outline_map_24,
        title = "Carte",
    )
)