package world.anhgelus.parismobility.plugin

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.appendEncodedPathSegments
import org.gradle.api.Plugin
import org.gradle.api.Project

class DataGeneratorPlugin : Plugin<Project> {
	val client = HttpClient(CIO)

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

	suspend fun request(
		builder: HttpRequestBuilder.() -> Unit,
	): ByteArray {
		return client.get { builder() }.bodyAsBytes()
	}

	suspend fun dataRequest(dataset: String): ByteArray {
		return request {
			url {
				protocol = URLProtocol.HTTPS
				host = "data.iledefrance-mobilites.fr"
				appendEncodedPathSegments("api/explore/v2.1/catalog/datasets", dataset)
			}
		}
	}

	suspend fun primRequest(
		token: String,
		sub: String,
		accept: String
	): ByteArray {
		return request {
			url {
				protocol = URLProtocol.HTTPS
				host = "prim.iledefrance-mobilites.fr"
				appendEncodedPathSegments("marketplace/ilico", sub)
			}
			headers {
				append(HttpHeaders.Accept, accept)
				append("apiKey", token)
			}
		}
	}
}