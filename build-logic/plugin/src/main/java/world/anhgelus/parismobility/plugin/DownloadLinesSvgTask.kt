package world.anhgelus.parismobility.plugin

import com.android.ide.common.vectordrawable.Svg2Vector
import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.io.path.Path
import kotlin.io.path.pathString

abstract class DownloadLinesSvgTask : DefaultTask() {
	@get:Input
	abstract val target: Property<String>

	@get:Input
	abstract val modes: ListProperty<String>

	@get:Input
	abstract val token: Property<String>

	@TaskAction
	fun action() {
		logger.info("Downloading SVGs for ${modes.get().joinToString(separator = " ")}...")
		modes.get().forEach { get(it) }
		logger.info("SVGs downloaded")
	}

	private fun get(mode: String) {
		DataGeneratorPlugin.INSTANCE.primRequest(
			token.get(),
			"getIcon/sprite?usage=signage_spaces&format=zip_svg&style=colored&getAll=true&transportMode=$mode",
			"application/zip"
		).getOrThrow().inputStream().use { b ->
			ZipInputStream(b).use { ins ->
				var ze: ZipEntry? = null
				while (ins.nextEntry.also { ze = it } != null) {
					ins.readAllBytes().let {
						val name = lineFileName(ze!!.name.split(":")[2].split(".")[0])
						val f = File(Path(target.get(), "$name.xml").pathString)
						if (!f.createNewFile()) continue
						val p = Path(
							project.layout.buildDirectory.asFile.get().path,
							"tmp",
							"$name.svg"
						)
						val tmp = File(p.pathString)
						tmp.createNewFile()
						val err = Svg2Vector.parseSvgToXml(p, f.outputStream())
						if (!err.isEmpty()) throw Exception(err)
					}
				}
			}
		}
	}

	companion object {
		fun lineFileName(name: String): String = "line_${name.lowercase()}"
	}
}