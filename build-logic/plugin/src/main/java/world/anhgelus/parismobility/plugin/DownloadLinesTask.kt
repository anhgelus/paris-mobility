package world.anhgelus.parismobility.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class DownloadLinesTask : DefaultTask() {
	@get:Input
	abstract val target: Property<String>

	@TaskAction
	fun action() {
		logger.info("Downloading lines...")
		val f = File(target.get())
		f.createNewFile()
		DataGeneratorPlugin.INSTANCE
			.dataRequest("emplacement-des-gares-idf")
			.getOrThrow()
			.let { f.writeBytes(it) }
		logger.info("Lines downloaded...")
	}
}