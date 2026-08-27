package world.anhgelus.parismobility.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
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
		target.tasks.register("downloadData") {
			group = "data"
			description = "Download required data"

			dependsOn(dlLinesSvg.name, dlPlan.name, dlLines.name)
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
}