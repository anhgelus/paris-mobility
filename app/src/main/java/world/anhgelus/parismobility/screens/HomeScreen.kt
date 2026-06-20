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
import world.anhgelus.parismobility.models.HomeViewModel
import world.anhgelus.parismobility.ui.LineKind
import world.anhgelus.parismobility.ui.ScreenTitle
import world.anhgelus.parismobility.ui.StopsMonitoring
import world.anhgelus.parismobility.models.LineKind as LK

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val lines by viewModel.lines.collectAsStateWithLifecycle()
    val stops by viewModel.stops.collectAsStateWithLifecycle()
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        ScreenTitle("Votre réseau")
        LineKind(
            modifier = Modifier.padding(horizontal = 16.dp),
            name = "État de vos lignes",
            lines = lines,
            kind = LK.RER,
            onClick = { _, _ -> },
        )
        StopsMonitoring(stops)
    }
}