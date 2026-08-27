package world.anhgelus.parismobility.plugin

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class DownloadPlansTask : DefaultTask() {
	@get:Input
	abstract val target: Property<String>

	@TaskAction
	fun action() {
		logger.info("Downloading metro plan...")
		runBlocking {
			DataGeneratorPlugin.INSTANCE
				.client
				.get("https://www.ratp.fr/sites/default/files/plans-lignes/Plans-essentiels/Plan-Metro.1772790495.png")
				.bodyAsBytes()
		}.let { File(target.get()).writeBytes(it) }
		logger.info("Plan downloaded...")
	}
}