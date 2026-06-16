package world.anhgelus.parismobility.navigation

import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation3.runtime.NavKey

@Composable
fun NavigationBar(
    selectedKey: NavKey,
    modifier: Modifier = Modifier,
    onSelectKey: (NavKey) -> Unit
) {
    BottomAppBar(modifier = modifier) {
        TOP_LEVEL_DESTINATIONS.forEach { (key, route) ->
            NavigationBarItem(
                selected = selectedKey == key,
                onClick = { onSelectKey(key) },
                icon = {
                    Icon(
                        painter = painterResource(route.painterID),
                        contentDescription = route.title
                    )
                },
                label = {
                    Text(text = route.title)
                }
            )
        }
    }
}