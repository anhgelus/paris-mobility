package world.anhgelus.parismobility.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import world.anhgelus.parismobility.screens.HomeScreen

@Composable
fun NavigationRoute(
    modifier: Modifier = Modifier
) {
    val navigator = remember {
        Navigator(Route.Home, TOP_LEVEL_DESTINATIONS.keys)
    }
    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar(selectedKey = navigator.current()) { navigator.go(it) }
        }
    ) { innerPadding ->
        NavDisplay(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            backStack = navigator.backStack(),
            entryProvider = { key: NavKey ->
                when (key) {
                    is Route.Home -> {
                        NavEntry(key) {
                            HomeScreen()
                        }
                    }

                    else -> error("unknown key $key")
                }
            }
        )
    }
}