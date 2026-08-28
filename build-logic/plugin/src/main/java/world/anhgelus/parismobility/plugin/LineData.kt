package world.anhgelus.parismobility.plugin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Line(
	@SerialName("id_line") val id: String,
	@SerialName("name_line") val name: String,
	@SerialName("shortname_line") val shortName: String = name,
	@SerialName("transportmode") val mode: TransportMode,
	@SerialName("transportsubmode") val submode: String? = null,
	@SerialName("id_groupoflines") val groupOfLines: String? = null,
	@SerialName("networkname") val network: String? = null,
	@SerialName("status") val status: Status = Status.INACTIVE,
	@SerialName("colourweb_hexa") val rawColor: String,
	@SerialName("textcolourweb_hexa") val rawTextColor: String,
) {
	enum class TransportMode {
		@SerialName("bus")
		BUS,

		@SerialName("rail")
		RAIL,

		@SerialName("funicular")
		FUNICULAR,

		@SerialName("metro")
		METRO,

		@SerialName("tram")
		TRAM,

		@SerialName("cableway")
		CABLEWAY,

		@SerialName("water")
		WATER
	}

	enum class Status {
		@SerialName("active")
		ACTIVE,

		@SerialName("prochainement active")
		INACTIVE
	}
}

@Serializable
data class Stop(
	@SerialName("id_gares") val id: Int,
	@SerialName("idrefligc") val line: String,
	@SerialName("nom_iv") val name: String,
	@SerialName("id_ref_zda") val zda: Int
)