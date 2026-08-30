package world.anhgelus.parismobility.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateDataTask : DefaultTask() {
	@get:OutputDirectory
	abstract val outputDirectory: DirectoryProperty

	@TaskAction
	fun action() {
		val plugin = DataGeneratorPlugin.INSTANCE
		val lines = plugin.dataRequest("referentiel-des-lignes")
			.getOrThrow()
			.let { plugin.json.decodeFromString<List<Line>>(it.decodeToString()) }
			.filter { it.status == Line.Status.ACTIVE }
			.fold(mutableMapOf<Line.TransportMode, MutableList<Line>>()) { acc, line ->
				val lines = acc[line.mode] ?: mutableListOf()
				lines.add(line)
				acc[line.mode] = lines
				acc
			}
			.let { map ->
				val acc = StringBuilder()
				acc.append("val MODES = mapOf(\n")
				map.forEach { (mode, lines) ->
					if (mode == Line.TransportMode.BUS || mode == Line.TransportMode.CABLEWAY || mode == Line.TransportMode.FUNICULAR)
						return@forEach
					acc.append("\tLine.TransportMode.${mode} to mapOf(\n")
					lines.forEach {
						if ((it.network == null && it.mode == Line.TransportMode.RAIL) ||
							(it.network == "TER")
						) return@forEach
						acc.append(
							"""
							|		"${it.id}" to Line(
							|			"${it.id}", 
							|			"${it.name}",
							|			"${it.shortName}",
							|			Line.TransportMode.${it.mode},
							|			${it.submode?.let { s -> "\"$s\"" }},
							|			${it.groupOfLines?.let { s -> "\"$s\"" }},
							|			${it.network.let { s -> "\"$s\"" }},
							|			Line.Status.${it.status},
							|			Color("#${it.rawColor}".toColorInt()),
							|			Color("#${it.rawTextColor}".toColorInt()),
							|			R.drawable.${
								if (it.name != "V") DownloadLinesSvgTask.lineFileName(it.id)
								else "v"
							},
							|		),
							|""".trimMargin()
						)
					}
					acc.append("\t),\n")
				}
				acc.append(")\n")
				acc.toString()
			}
		val stops = plugin.dataRequest("emplacement-des-gares-idf")
			.getOrThrow()
			.let { plugin.json.decodeFromString<List<Stop>>(it.decodeToString()) }
			.fold(mutableMapOf<String, MutableList<Stop>>()) { acc, stop ->
				val lines = acc[stop.line] ?: mutableListOf()
				lines.add(stop)
				acc[stop.line] = lines
				acc
			}
			.let { map ->
				val acc = StringBuilder()
				acc.append("val STOPS = mapOf(\n")
				map.forEach { (line, stops) ->
					acc.append("\t\"$line\" to mapOf(\n")
					stops.forEach {
						acc.append(
							"""|		${it.id} to Stop(${it.id}, "${it.line}", "${it.name}", ${it.zda}),
						|""".trimMargin()
						)
					}
					acc.append("\t),\n")
				}
				acc.append(")\n")
				acc
			}
		outputDirectory.get().asFile.let {
			it.mkdirs()
			val parent = File(it, "world/anhgelus/parismobility/data").also { p ->
				p.mkdirs()
			}
			File(parent, "LineData.kt").writeText(template + lines + stops)
		}
	}
}

const val template = """package world.anhgelus.parismobility.data

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import world.anhgelus.parismobility.R

@Serializable
data class Line(
	val id: String,
	val name: String,
	val shortName: String = name,
	val mode: TransportMode,
	val submode: String? = null,
	val groupOfLines: String? = null,
	val network: String? = null,
	val status: Status = Status.INACTIVE,
	@Transient val color: Color = Color.White,
	@Transient val textColor: Color = Color.Black,
	val icon: Int,
) {
	@Serializable
	enum class TransportMode {
		BUS,
		RAIL,
		FUNICULAR,
		METRO,
		TRAM,
		CABLEWAY,
		WATER
	}

	@Serializable
	enum class Status {
		ACTIVE,
		INACTIVE
	}
}

@Serializable
data class Stop(
	val id: Int,
	val line: String,
	val name: String,
	val zda: Int
)

"""