package world.anhgelus.parismobility.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class DataGeneratorPlugin : Plugin<Project> {
	val client: HttpClient = HttpClient.newHttpClient()

	override fun apply(target: Project) {
		INSTANCE = this
		val dlPlan = target.tasks.register("downloadPlans", DownloadPlansTask::class.java) {
			group = "data"
			description = "Download plans"
		}
		val dlLinesSvg =
			target.tasks.register("downloadLinesSvg", DownloadLinesSvgTask::class.java) {
				group = "data"
				description = "Download lines SVG"
			}
		val dlLines = target.tasks.register("downloadLines", DownloadLinesTask::class.java) {
			group = "data"
			description = "Download lines"
		}
		val dlStops = target.tasks.register("downloadStops", DownloadStopsTask::class.java) {
			group = "data"
			description = "Download stops"
		}
		target.tasks.register("downloadData") {
			group = "data"
			description = "Download required data"

			dependsOn(dlLinesSvg.name, dlPlan.name, dlLines.name, dlStops.name)
		}
	}

	companion object {
		lateinit var INSTANCE: DataGeneratorPlugin
			private set
	}

	fun request(
		url: String,
		builder: (HttpRequest.Builder) -> HttpRequest.Builder = { it }
	): Result<ByteArray> {
		return HttpRequest.newBuilder(URI(url))
			.GET()
			.let { builder(it) }
			.let {
				client.send(it.build(), HttpResponse.BodyHandlers.ofByteArray())
			}.let {
				if (it.statusCode() >= 400) Result.failure(Exception("Invalid status code: ${it.statusCode()}"))
				else Result.success(it.body())
			}
	}

	fun dataRequest(dataset: String): Result<ByteArray> {
		return request("https://data.iledefrance-mobilites.fr/api/explore/v2.1/catalog/datasets/$dataset/exports/json")
	}

	fun primRequest(
		token: String,
		sub: String,
		accept: String
	): Result<ByteArray> {
		return request("https://prim.iledefrance-mobilites.fr/marketplace/ilico/$sub") {
			it.headers("Accept", accept, "apiKey", token)
		}
	}

	abstract class DataDownload(
		@Internal val dataName: String,
		@Internal val dataset: String,
	) : DefaultTask() {
		@get:Input
		abstract val target: Property<String>

		@TaskAction
		fun action() {
			logger.info("Downloading $dataName...")
			val f = File(target.get())
			f.createNewFile()
			INSTANCE.dataRequest(dataset)
				.getOrThrow()
				.let { f.writeBytes(it) }
			logger.info("$dataName downloaded.")
		}
	}
}

abstract class DownloadLinesTask :
	DataGeneratorPlugin.DataDownload("lines", "referentiel-des-lignes")

abstract class DownloadStopsTask :
	DataGeneratorPlugin.DataDownload("stops", "emplacement-des-gares-idf")