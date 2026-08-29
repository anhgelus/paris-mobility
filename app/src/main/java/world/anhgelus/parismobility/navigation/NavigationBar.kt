package world.anhgelus.parismobility.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import world.anhgelus.parismobility.R

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

@Composable
fun NavigationFloatingButton(stack: NavBackStack<NavKey>) {
	val btn = stack.last().let { it as Route }.getButton() ?: return
	FilledIconButton(
		onClick = { btn.onClick(stack) },
		colors = IconButtonDefaults.filledIconButtonColors(MaterialTheme.colorScheme.primaryContainer),
		modifier = Modifier.size(64.dp),
		shape = ShapeDefaults.Large,
	) {
		Icon(
			painter = painterResource(btn.icon),
			contentDescription = btn.contentDescription,
			modifier = Modifier.size(32.dp)
		)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationTopBar(stack: NavBackStack<NavKey>) {
	val bar = stack.last().let { it as Route }.getTopBar() ?: return
	TopAppBar(
		title = { Text(bar.title) },
		navigationIcon = {
			IconButton(onClick = { bar.onBack(stack) }) {
				Icon(
					painter = painterResource(R.drawable.outline_arrow_back_24),
					contentDescription = "Retour arrière",
				)
			}
		}
	)
}