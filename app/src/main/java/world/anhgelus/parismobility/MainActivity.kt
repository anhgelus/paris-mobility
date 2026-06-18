package world.anhgelus.parismobility

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import world.anhgelus.parismobility.data.LinesDataSource
import world.anhgelus.parismobility.data.LinesRepository
import world.anhgelus.parismobility.navigation.NavigationRoot
import world.anhgelus.parismobility.ui.theme.ParisMobiliteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val linesRepo = LinesRepository(LinesDataSource)
        val ctx = this

        setContent {
            LaunchedEffect(true) {
                linesRepo.updateLines(ctx)
            }
            ParisMobiliteTheme {
                NavigationRoot(linesRepo)
            }
        }
    }
}