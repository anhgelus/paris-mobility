package world.anhgelus.parismobility.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import world.anhgelus.parismobility.data.Disruption
import world.anhgelus.parismobility.data.Disruptions
import world.anhgelus.parismobility.data.Line
import world.anhgelus.parismobility.models.LineKind
import world.anhgelus.parismobility.ui.theme.Typography
import java.time.LocalDateTime

private fun Int.toStringDate(): String {
	var h = this.toString()
	if (h.length < 2) h = "0$h"
	return h
}

fun formatLocalDateTime(l: LocalDateTime): String {
	return "${l.dayOfMonth.toStringDate()}/${l.monthValue.toStringDate()}/${l.year.toStringDate()}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisruptionsDrawer(
	kind: LineKind,
	line: Line,
	disruptions: Disruptions,
	modifier: Modifier = Modifier,
	sheetState: SheetState = rememberModalBottomSheetState(),
	onDismissRequest: () -> Unit
) {
	ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
		LazyColumn(
			verticalArrangement = Arrangement.spacedBy(16.dp),
			modifier = modifier.fillMaxHeight(),
		) {
			item {
				Text(
					text = "Incidents sur ${kind.displayName} ${line.name}",
					style = Typography.headlineMedium,
				)
			}
			val dis = disruptions[line.id]?.sorted()
			if (dis.isNullOrEmpty()) {
				item { Text("Aucun incident trouvé.") }
			} else {
				items(dis, key = { it.id }) { DisruptionCard(it) }
			}
			item { Spacer(modifier = Modifier.padding(bottom = 16.dp)) }
		}
	}
}

@Composable
fun DisruptionCard(disruption: Disruption, modifier: Modifier = Modifier) {
	val color = disruption.severity.color
	val enabled = disruption.isHappening()
	Card(
		border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
		colors = CardDefaults.cardColors(
			contentColor = MaterialTheme.colorScheme.onSurface,
			containerColor = MaterialTheme.colorScheme.surface
		),
		modifier = modifier
			.fillMaxWidth()
			.padding(top = 8.dp),
	) {
		val modifier = Modifier.padding(16.dp)
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.background(if (enabled) color.first else MaterialTheme.colorScheme.surfaceVariant)
				.padding(16.dp)
		) {
			Column {
				SectionTitle(
					content = disruption.title,
					color = if (enabled) color.second else MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.padding(bottom = 8.dp)
				)
				val p = disruption.periods.first()
				Text(
					text = "Du ${formatLocalDateTime(p.begin)} au ${formatLocalDateTime(p.end)}",
					style = Typography.bodySmall,
					color = if (enabled) color.second else MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
		}
		Text(AnnotatedString.fromHtml(disruption.message), modifier)
	}
}