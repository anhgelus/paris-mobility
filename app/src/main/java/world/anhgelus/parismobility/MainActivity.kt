package world.anhgelus.parismobility

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.rememberNavBackStack
import world.anhgelus.parismobility.models.GeneralViewModel
import world.anhgelus.parismobility.navigation.NavigationBar
import world.anhgelus.parismobility.navigation.NavigationFloatingButton
import world.anhgelus.parismobility.navigation.NavigationRoot
import world.anhgelus.parismobility.navigation.NavigationTopBar
import world.anhgelus.parismobility.navigation.Route
import world.anhgelus.parismobility.ui.theme.ParisMobiliteTheme

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		enableEdgeToEdge()
		super.onCreate(savedInstanceState)

		setContent {
			ParisMobiliteTheme {
				val model = viewModel { GeneralViewModel(baseContext) }
				val rootBackStack = rememberNavBackStack(Route.Home)
				val homeBackStack = rememberNavBackStack(Route.Home)
				val rootKey = rootBackStack.last()
				val stack = when (rootKey) {
					is Route.HomeRoute -> homeBackStack
					else -> rootBackStack
				}
				Scaffold(
					topBar = { NavigationTopBar(stack) },
					floatingActionButton = { NavigationFloatingButton(stack) },
					bottomBar = { NavigationBar(rootKey) { rootBackStack += it } },
				) { innerPadding ->
					val connected by model.isConnected.collectAsStateWithLifecycle()
					Column(modifier = Modifier.padding(innerPadding)) {
						if (!connected) {
							Row(
								modifier = Modifier
									.fillMaxWidth()
									.shadow(1.dp)
									.background(MaterialTheme.colorScheme.errorContainer)
									.padding(16.dp),
								verticalAlignment = Alignment.CenterVertically,
								horizontalArrangement = Arrangement.spacedBy(8.dp),
							) {
								Icon(
									painter = painterResource(R.drawable.outline_signal_cellular_connected_no_internet_0_bar_24),
									contentDescription = null,
									tint = MaterialTheme.colorScheme.onErrorContainer,
								)
								Text(
									text = stringResource(R.string.disconnected),
									color = MaterialTheme.colorScheme.onErrorContainer,
								)
							}
						}
						NavigationRoot(
							baseContext,
							model,
							rootBackStack,
							homeBackStack,
						)
					}
				}
			}
		}
	}
}