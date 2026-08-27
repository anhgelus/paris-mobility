package world.anhgelus.parismobility.plugin

import com.android.ide.common.vectordrawable.Svg2Vector
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.zip.ZipInputStream
import kotlin.io.path.Path
import kotlin.io.path.pathString

abstract class UpdateLinesSvgTask : DefaultTask() {
	@get:Input
	abstract val target: Property<String>

	@get:Input
	abstract val modes: Property<List<String>>

	@get:Input
	abstract val token: Property<String>

	@TaskAction
	fun action() {
		logger.info("Downloading svg for ${modes.get().joinToString(separator = " ")}...")
		modes.get().forEach { get(it) }
		logger.info("SVG downloaded")
	}

	private fun get(mode: String) {
		val b = runBlocking {
			DataGeneratorPlugin.INSTANCE.primRequest(
				token.get(),
				"getIcon/sprite?usage=signage_spaces&format=zip_svg&style=colored&getAll=true&transportMode=$mode",
				"application/zip"
			)
		}
		ZipInputStream(b.inputStream()).use { ins ->
			var ze = ins.nextEntry
			while (ze != null) {
				ins.readAllBytes().let {
					val name = ze.name.split(":")[2].split(".")[0].lowercase()
					val f = File(Path(target.get(), "line_$name.xml").pathString)
					if (!f.createNewFile()) continue
					val p = Path(project.buildTreePath, "tmp", "$name.svg")
					val tmp = File(p.pathString)
					tmp.createNewFile()
					val err = Svg2Vector.parseSvgToXml(p, f.outputStream())
					if (!err.isEmpty()) throw Exception(err)
				}
				ze = ins.nextEntry
			}
		}
	}
}