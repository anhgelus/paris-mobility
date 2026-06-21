package world.anhgelus.parismobility

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import world.anhgelus.parismobility.data.LinesDataSource
import world.anhgelus.parismobility.data.PrimDataSource
import world.anhgelus.parismobility.models.GeneralViewModel
import world.anhgelus.parismobility.navigation.NavigationRoot
import world.anhgelus.parismobility.ui.theme.ParisMobiliteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            ParisMobiliteTheme {
                val model = viewModel {
                    GeneralViewModel(baseContext, LinesDataSource, PrimDataSource)
                }
                NavigationRoot(baseContext, model)
            }

        }
    }
}