package world.anhgelus.parismobility

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import world.anhgelus.parismobility.data.LinesDataSource
import world.anhgelus.parismobility.data.LinesRepository
import world.anhgelus.parismobility.data.PreferencesRepository
import world.anhgelus.parismobility.data.PrimDataSource
import world.anhgelus.parismobility.navigation.NavigationRoot
import world.anhgelus.parismobility.ui.theme.ParisMobiliteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val scope = rememberCoroutineScope()
            val linesRepo = LinesRepository(scope, LinesDataSource, PrimDataSource)
            val preferencesRepo = PreferencesRepository(this)
            LaunchedEffect(true) {
                linesRepo.initLines(resources)
            }
            ParisMobiliteTheme {
                NavigationRoot(this, preferencesRepo, linesRepo)
            }
        }
    }
}