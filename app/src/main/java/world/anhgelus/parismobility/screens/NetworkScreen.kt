package world.anhgelus.parismobility.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import world.anhgelus.parismobility.data.Disruptions
import world.anhgelus.parismobility.data.Line
import world.anhgelus.parismobility.models.NetworkViewModel
import world.anhgelus.parismobility.ui.DisruptionsDrawer
import world.anhgelus.parismobility.ui.LineKind
import world.anhgelus.parismobility.ui.ScreenTitle
import world.anhgelus.parismobility.models.LineKind as LK

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(
	viewModel: NetworkViewModel,
	disruptions: Disruptions,
	modifier: Modifier = Modifier,
) {
	val groups by viewModel.lines.collectAsStateWithLifecycle()
	var selected by remember { mutableStateOf<Pair<LK, Line>?>(null) }
	LazyColumn(
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(32.dp),
	) {
		item { ScreenTitle("Réseau") }
		items(items = groups.filter { (key, _) -> key != LK.BUS }.toList()) { (kind, lines) ->
			LineKind(
				modifier = Modifier
					.padding(horizontal = 16.dp)
					.padding(bottom = 16.dp),
				name = kind.displayName,
				lines = lines.values,
				kind = kind,
				onClick = { kind, line -> selected = kind to line.line }
			)
		}
	}
	selected?.let { (kind, line) ->
		DisruptionsDrawer(kind, line, disruptions, modifier) { selected = null }
	}
}