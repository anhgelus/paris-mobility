package world.anhgelus.parismobility.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import world.anhgelus.parismobility.R
import world.anhgelus.parismobility.models.LineKind

@Serializable
sealed interface Route : NavKey {
	fun name(): String?

	@Serializable
	open class Helper(val name: String? = null) : Route {
		override fun name(): String? = name
	}

	@Serializable
	data object Home : Helper("Votre réseau") {
		@Serializable
		data object Modify : Helper() {
			@Serializable
			data object Stops : Helper()

			@Serializable
			data class Stop(val kind: LineKind, val lineId: String) : Helper()
		}
	}

	@Serializable
	data object Network : Helper("Réseau") {
		@Serializable
		data class SpecificLine(val kind: LineKind, val lineId: String) : Helper()
	}

	@Serializable
	data object Map : Helper()
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