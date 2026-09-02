package world.anhgelus.parismobility

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import world.anhgelus.parismobility.models.GeneralViewModel
import world.anhgelus.parismobility.models.LineKind
import world.anhgelus.parismobility.ui.DisruptionsList
import world.anhgelus.parismobility.ui.theme.ParisMobiliteTheme

class LineDisruptionActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		enableEdgeToEdge()
		super.onCreate(savedInstanceState)

		val bundle = intent.extras!!
		val kind = LineKind.entries[bundle.getByte(KIND_KEY).toInt()]
		val line = bundle.getString(LINE_KEY)

		setContent {
			ParisMobiliteTheme(isSystemInDarkTheme()) {
				val model = viewModel { GeneralViewModel(baseContext) }
				Scaffold { innerPadding ->
					val lines by model.linesRepository.lines.collectAsStateWithLifecycle()
					val disruptions by model.linesRepository.disruptions.collectAsStateWithLifecycle(
						emptyMap()
					)
					DisruptionsList(
						kind,
						lines[kind]!![line]!!.line,
						disruptions,
						Modifier
							.padding(innerPadding)
							.padding(10.dp)
					)
				}
			}
		}
	}

	companion object {
		const val KIND_KEY = "kind"
		const val LINE_KEY = "line"
	}
}