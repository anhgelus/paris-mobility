package world.anhgelus.parismobility.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import world.anhgelus.parismobility.models.NetworkViewModel
import world.anhgelus.parismobility.ui.LineKind
import world.anhgelus.parismobility.ui.ScreenTitle

@Composable
fun NetworkScreen(
    viewModel: NetworkViewModel,
    modifier: Modifier = Modifier,
) {
    val groups by viewModel.linesGroups.collectAsStateWithLifecycle()
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        ScreenTitle("Réseau")
        groups.forEach { (kind, lines) ->
            LineKind(
                modifier = Modifier.padding(horizontal = 16.dp),
                name = kind.displayName,
                lines = lines
            )
        }
    }
}