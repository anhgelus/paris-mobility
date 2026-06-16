package world.anhgelus.parismobility.navigation

import world.anhgelus.parismobility.R

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