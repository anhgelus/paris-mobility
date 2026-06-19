package world.anhgelus.parismobility

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import world.anhgelus.parismobility.data.LinesDataSource
import world.anhgelus.parismobility.data.LinesRepository
import world.anhgelus.parismobility.data.PrimDataSource
import world.anhgelus.parismobility.navigation.NavigationRoot
import world.anhgelus.parismobility.ui.theme.ParisMobiliteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val linesRepo = LinesRepository(LinesDataSource, PrimDataSource)

        val ctx = this

        setContent {
            LaunchedEffect(true) {
                linesRepo.initLines(ctx)
                CoroutineScope(Dispatchers.IO).launch {
                    linesRepo.updateDisruptions()
                }
            }
            ParisMobiliteTheme {
                NavigationRoot(linesRepo)
            }
        }
    }
}