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
		val contentDescription: Int,
		val onClick: (NavBackStack<NavKey>) -> Unit
	)

	fun getButton(): Data? = null

	data class TopBarData(val title: Int, val onBack: (NavBackStack<NavKey>) -> Unit)

	fun getTopBar(): TopBarData? = null

	interface HomeRoute : Route

	@Serializable
	data object Home : HomeRoute {
		override fun getButton(): Data = Data(
			icon = R.drawable.outline_edit_24,
			contentDescription = R.string.button_modify,
		) { it.add(Modify) }

		@Serializable
		data object Modify : HomeRoute {
			override fun getTopBar(): TopBarData = TopBarData(R.string.home_modify) {
				it.removeAt(it.lastIndex)
			}

			@Serializable
			data object Stops : Route

			@Serializable
			data class Stop(val kind: LineKind, val lineId: String) : Route
		}
	}

	@Serializable
	data object Network : Route

	@Serializable
	data object Map : Route
}

data class BottomNavItem(
	val painterID: Int,
	val title: Int,
)

val TOP_LEVEL_DESTINATIONS = mapOf(
	Route.Home to BottomNavItem(
		painterID = R.drawable.outline_home_24,
		title = R.string.button_home,
	),
	Route.Network to BottomNavItem(
		painterID = R.drawable.outline_train_24,
		title = R.string.button_network,
	),
	Route.Map to BottomNavItem(
		painterID = R.drawable.outline_map_24,
		title = R.string.button_map,
	)
)