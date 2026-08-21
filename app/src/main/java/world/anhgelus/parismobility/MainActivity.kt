package world.anhgelus.parismobility

import android.net.ConnectivityManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.rememberNavBackStack
import world.anhgelus.parismobility.data.BackendDataSource
import world.anhgelus.parismobility.data.LinesDataSource
import world.anhgelus.parismobility.models.GeneralViewModel
import world.anhgelus.parismobility.navigation.NavigationBar
import world.anhgelus.parismobility.navigation.NavigationRoot
import world.anhgelus.parismobility.navigation.Route
import world.anhgelus.parismobility.ui.theme.ParisMobiliteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val conn = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        setContent {
            ParisMobiliteTheme {
                val model = viewModel {
                    GeneralViewModel(
                        baseContext,
                        LinesDataSource,
                        BackendDataSource(conn)
                    )
                }
                val rootBackStack = rememberNavBackStack(Route.Home)
                // disable return function for main nav
                Scaffold(
                    bottomBar = {
                        NavigationBar(selectedKey = rootBackStack.last()) { rootBackStack.add(it) }
                    }
                ) { innerPadding ->
                    val loading by model.isLoading.collectAsStateWithLifecycle()
                    if (loading) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(64.dp),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    } else {
                        NavigationRoot(baseContext, model, rootBackStack, innerPadding)
                    }
                }
            }
        }
    }
}