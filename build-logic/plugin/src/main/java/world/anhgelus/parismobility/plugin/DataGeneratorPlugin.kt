package world.anhgelus.parismobility.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets.UTF_8

class DataGeneratorPlugin : Plugin<Project> {
	val client: HttpClient = HttpClient.newHttpClient()

	override fun apply(target: Project) {
		INSTANCE = this
		val dlPlan = target.tasks.register("downloadPlans", DownloadPlansTask::class.java) {
			group = "data"
			description = "Download plans"
		}
		val upLines = target.tasks.register("updateLinesSvg", UpdateLinesSvgTask::class.java) {
			group = "data"
			description = "Update lines SVG"
		}
		target.tasks.register("updateData") {
			group = "data"
			description = "Update every data"

			dependsOn(upLines.name, dlPlan.name)
		}
	}

	companion object {
		lateinit var INSTANCE: DataGeneratorPlugin
			private set
	}

	fun request(
		url: String,
		builder: (HttpRequest.Builder) -> HttpRequest.Builder = { it }
	): String {
		return HttpRequest.newBuilder(URI(url)).GET()
			.let { builder(it) }
			.let {
				client.send(it.build(), HttpResponse.BodyHandlers.ofString(UTF_8))
					.body()
			}
	}

	fun dataRequest(dataset: String): String {
		return request("https://data.iledefrance-mobilites.fr/api/explore/v2.1/catalog/datasets/$dataset")
	}

	fun primRequest(
		token: String,
		sub: String,
		accept: String
	): String {
		return request("https://prim.iledefrance-mobilites.fr/marketplace/ilico/$sub") {
			it.headers("Accept", accept, "apiKey", token)
		}
	}
}