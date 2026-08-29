package world.anhgelus.parismobility

import android.net.ConnectivityManager
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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.rememberNavBackStack
import world.anhgelus.parismobility.data.LinesDataSource
import world.anhgelus.parismobility.data.backend.BackendConnection
import world.anhgelus.parismobility.data.backend.BackendDataSource
import world.anhgelus.parismobility.models.GeneralViewModel
import world.anhgelus.parismobility.navigation.NavigationBar
import world.anhgelus.parismobility.navigation.NavigationRoot
import world.anhgelus.parismobility.navigation.Route
import world.anhgelus.parismobility.navigation.getLastRouteKey
import world.anhgelus.parismobility.ui.theme.ParisMobiliteTheme

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		enableEdgeToEdge()
		super.onCreate(savedInstanceState)

		val conn = BackendConnection(getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager)

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
				val homeBackStack = rememberNavBackStack(Route.Home)
				val key = getLastRouteKey(rootBackStack, homeBackStack)
				Scaffold(
					bottomBar = {
						NavigationBar(rootBackStack.last()) { rootBackStack.add(it) }
					},
					floatingActionButton = {
						val btn = key.getButton() ?: return@Scaffold
						val onClick = { btn.onClick(homeBackStack) }
						FilledIconButton(
							onClick = onClick,
							colors = IconButtonDefaults.filledIconButtonColors(MaterialTheme.colorScheme.primaryContainer),
							modifier = Modifier.size(64.dp),
							shape = ShapeDefaults.Large,
						) {
							Icon(
								painter = painterResource(btn.icon),
								contentDescription = btn.contentDescription,
								modifier = Modifier.size(32.dp)
							)
						}
					},
				) { innerPadding ->
					val connected by conn.isConnected.collectAsStateWithLifecycle()
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
									text = "Déconnecté du serveur.",
									color = MaterialTheme.colorScheme.onErrorContainer,
								)
							}
						}
						NavigationRoot(baseContext, model, rootBackStack, homeBackStack)
					}
				}
			}
		}
	}
}