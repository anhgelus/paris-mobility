package world.anhgelus.parismobility.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import world.anhgelus.parismobility.R
import world.anhgelus.parismobility.models.LineKind

@Serializable
sealed interface Route : NavKey {
	data class Data(
		val icon: Int,
		val contentDescription: String,
		val onClick: (NavBackStack<NavKey>) -> Unit
	)

	fun getButton(): Data? = null

	data class TopBarData(val title: String, val onBack: (NavBackStack<NavKey>) -> Unit)

	fun getTopBar(): TopBarData? = null

	@Serializable
	data object Home : Route {
		override fun getButton(): Data = Data(
			icon = R.drawable.outline_edit_24,
			contentDescription = "Modifier votre réseau",
		) { it.add(Modify) }

		@Serializable
		data object Modify : Route {
			@Serializable
			data object Stops : Route

			@Serializable
			data class Stop(val kind: LineKind, val lineId: String) : Route
		}
	}

	@Serializable
	data object Network : Route {
		@Serializable
		data class SpecificLine(val kind: LineKind, val lineId: String) : Route
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

fun getLastRouteKey(
	backStack: NavBackStack<NavKey>,
	homeBackStack: NavBackStack<NavKey>,
): Route {
	return when (val general = backStack.last() as Route) {
		is Route.Home -> homeBackStack.last() as Route
		else -> general
	}
}