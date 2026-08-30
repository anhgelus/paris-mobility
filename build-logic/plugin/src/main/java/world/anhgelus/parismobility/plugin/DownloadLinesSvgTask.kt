package world.anhgelus.parismobility.plugin

import com.android.ide.common.vectordrawable.Svg2Vector
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.io.path.Path
import kotlin.io.path.pathString

abstract class DownloadLinesSvgTask : DefaultTask() {
	@get:Input
	abstract val modes: ListProperty<String>

	@get:Input
	abstract val token: Property<String>

	@get:OutputDirectory
	abstract val outputDirectory: DirectoryProperty

	@TaskAction
	fun action() {
		logger.info("Downloading SVGs for ${modes.get().joinToString(separator = " ")}...")
		modes.get().forEach { get(it) }
		logger.info("SVGs downloaded")

		logger.info("Downloading metro plan...")
		outputDirectory.get().asFile.let { out ->
			out.mkdirs()
			val parent = File(out, "drawable").also { it.mkdirs() }
			val f = File(parent, "plan_metro.png").also { it.createNewFile() }
			DataGeneratorPlugin.INSTANCE
				.request("https://www.ratp.fr/sites/default/files/plans-lignes/Plans-essentiels/Plan-Metro.1772790495.png")
				.getOrThrow()
				.let { f.writeBytes(it) }
		}
		logger.info("Plan downloaded.")
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
					val b = ins.readAllBytes()
					outputDirectory.get().asFile.let { out ->
						out.mkdirs()
						val parent = File(out, "drawable").also { it.mkdirs() }
						val name = lineFileName(ze!!.name.split(":")[2].split(".")[0])
						val f = File(Path(parent.path, "$name.xml").pathString)
						f.createNewFile()
						val p = Path(parent.path, "$name.svg")
						val tmp = File(p.pathString)
						tmp.createNewFile()
						tmp.writeBytes(b)
						val err = Svg2Vector.parseSvgToXml(p, f.outputStream())
						tmp.delete()
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