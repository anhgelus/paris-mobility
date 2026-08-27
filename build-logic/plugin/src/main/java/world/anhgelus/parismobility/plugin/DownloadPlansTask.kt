package world.anhgelus.parismobility.plugin

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
		val f = File(target.get())
		f.createNewFile()
		DataGeneratorPlugin.INSTANCE
			.request("https://www.ratp.fr/sites/default/files/plans-lignes/Plans-essentiels/Plan-Metro.1772790495.png")
			.getOrThrow()
			.let { f.writeBytes(it) }
		logger.info("Plan downloaded...")
	}
}