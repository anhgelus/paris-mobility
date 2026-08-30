package world.anhgelus.parismobility.plugin

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.AppPlugin
import kotlinx.serialization.json.Json
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.zip.GZIPInputStream
import kotlin.jvm.optionals.getOrNull

class DataGeneratorPlugin : Plugin<Project> {
	val client: HttpClient = HttpClient.newHttpClient()
	val json = Json { ignoreUnknownKeys = true }

	override fun apply(project: Project) {
		INSTANCE = this
		val dlPlan = project.tasks.register("downloadPlans", DownloadPlansTask::class.java) {
			group = "data"
			description = "Download plans"
		}
		val dlLinesSvg =
			project.tasks.register("downloadLinesSvg", DownloadLinesSvgTask::class.java) {
				group = "data"
				description = "Download lines SVG"
			}
		project.tasks.register("downloadData") {
			group = "data"
			description = "Download required data"

			dependsOn(dlLinesSvg.name, dlPlan.name)
		}
		project.plugins.withType(AppPlugin::class.java) {
			project.tasks.register("generateData", GenerateDataTask::class.java) {
				group = "data"
				description = "Generate data classes"
			}.let { task ->
				val components =
					project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
				components.onVariants { variant ->
					variant.sources
						.java
						?.addGeneratedSourceDirectory(task, GenerateDataTask::outputDirectory)
				}
			}
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
			.headers("Accept-Encoding", "gzip")
			.let { builder(it) }
			.let {
				client.send(it.build(), HttpResponse.BodyHandlers.ofByteArray())
			}.let {
				if (it.statusCode() >= 400) Result.failure(Exception("Invalid status code: ${it.statusCode()}"))
				else Result.success(it)
			}.mapCatching { old ->
				old.headers().firstValue("Content-Encoding").getOrNull()?.let { enc ->
					if (enc != "gzip") throw IllegalArgumentException("unknown encoding: $enc")
					old.body().inputStream().use { GZIPInputStream(it).readAllBytes() }
				} ?: old.body()
			}
	}

	fun dataRequest(dataset: String): Result<ByteArray> {
		return request("https://data.iledefrance-mobilites.fr/api/explore/v2.1/catalog/datasets/$dataset/exports/json") {
			it.headers("Content-Type", "application/json")
		}
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