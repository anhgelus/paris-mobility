package world.anhgelus.parismobility.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import world.anhgelus.parismobility.R
import world.anhgelus.parismobility.data.LineGroups
import world.anhgelus.parismobility.data.STOPS
import world.anhgelus.parismobility.data.SavedLine
import world.anhgelus.parismobility.data.SavedStop
import world.anhgelus.parismobility.data.Stop
import world.anhgelus.parismobility.data.contains
import world.anhgelus.parismobility.models.LineKind
import world.anhgelus.parismobility.navigation.Route
import world.anhgelus.parismobility.ui.LineDetailed
import world.anhgelus.parismobility.ui.SectionTitle

@Composable
fun ModifyScreen(
	groups: LineGroups,
	savedLines: Collection<SavedLine>,
	savedStops: Collection<SavedStop>,
	onUpdateLines: (LineKind, String, Boolean) -> Unit,
	onUpdateStops: (LineKind, String, Stop, Boolean) -> Unit,
	onClick: (PagerState, Int) -> Unit
) {
	val pager = rememberPagerState(0) { 2 }

	Column(modifier = Modifier.padding(horizontal = 16.dp)) {
		SecondaryTabRow(selectedTabIndex = pager.currentPage) {
			listOf(R.string.lines, R.string.stops).forEachIndexed { i, name ->
				Tab(
					selected = pager.currentPage == i,
					onClick = { onClick(pager, i) },
					text = { Text(stringResource(name)) }
				)
			}
		}
		HorizontalPager(state = pager) {
			when (it) {
				0 -> ModifySavedLines(groups, savedLines, onUpdateLines)
				1 -> ModifySavedStops(groups, savedStops, onUpdateStops)
			}
		}
	}
}

@Composable
fun ModifySavedLines(
	groups: LineGroups,
	savedLines: Collection<SavedLine>,
	onUpdate: (LineKind, String, Boolean) -> Unit,
) {
	LazyColumn(
		verticalArrangement = Arrangement.spacedBy(16.dp),
		modifier = Modifier.padding(horizontal = 16.dp)
	) {
		groups.filter { it.key != LineKind.BUS }.forEach { (kind, lines) ->
			item {
				SectionTitle(
					content = stringResource(kind.displayName),
					modifier = Modifier.padding(top = 16.dp)
				)
			}
			items(lines.values.toList()) {
				Select(
					checked = savedLines.contains(kind, it.line),
					onUpdate = { change ->
						onUpdate(kind, it.line.id, change)
					}
				) { m ->
					LineDetailed(kind, it.line, m)
				}
			}
		}
		item { Spacer(Modifier.padding(bottom = 16.dp)) }
	}
}

@Composable
fun ModifySavedStops(
	groups: LineGroups,
	savedStops: Collection<SavedStop>,
	onUpdateStops: (LineKind, String, Stop, Boolean) -> Unit,
) {
	val modifyBackStack = rememberNavBackStack(Route.Home.Modify.Stops)
	NavDisplay(
		backStack = modifyBackStack,
		entryProvider = entryProvider {
			entry<Route.Home.Modify.Stops> {
				LazyColumn(
					verticalArrangement = Arrangement.spacedBy(16.dp),
					modifier = Modifier.padding(horizontal = 16.dp)
				) {
					groups.filter { it.key != LineKind.BUS }.forEach { (kind, lines) ->
						item {
							SectionTitle(
								content = stringResource(kind.displayName),
								modifier = Modifier.padding(top = 16.dp)
							)
						}
						items(lines.values.toList()) {
							Row(
								modifier = Modifier
									.fillMaxWidth()
									.clickable(onClick = {
										modifyBackStack.add(
											Route.Home.Modify.Stop(
												kind,
												it.line.id
											)
										)
									}),
								horizontalArrangement = Arrangement.SpaceBetween,
								verticalAlignment = Alignment.CenterVertically,
							) {
								LineDetailed(kind, it.line)
								Icon(
									painter = painterResource(R.drawable.baseline_keyboard_arrow_right_24),
									contentDescription = "Continue"
								)
							}
						}
					}
					item { Spacer(Modifier.padding(bottom = 16.dp)) }
				}
			}
			entry<Route.Home.Modify.Stop> { (kind, line) ->
				LazyColumn(
					verticalArrangement = Arrangement.spacedBy(16.dp),
					modifier = Modifier
						.padding(16.dp)
				) {
					items(STOPS[line]?.values?.sortedBy { it.name } ?: listOf()) {
						Select(
							checked = savedStops.contains(kind, it),
							onUpdate = { change ->
								onUpdateStops(kind, line, it, change)
							}
						) { m ->
							Text(
								text = it.name,
								softWrap = true,
								modifier = m.fillMaxWidth()
							)
						}
					}
					item { Spacer(Modifier.padding(bottom = 16.dp)) }
				}
			}
		}
	)
}

@Composable
fun Select(
	checked: Boolean,
	onUpdate: (Boolean) -> Unit,
	content: @Composable (Modifier) -> Unit,
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = { onUpdate(!checked) }),
		horizontalArrangement = Arrangement.SpaceBetween,
		verticalAlignment = Alignment.CenterVertically,
	) {
		content(Modifier.weight(3f))
		Switch(
			checked = checked,
			onCheckedChange = { change -> onUpdate(change) },
			modifier = Modifier.weight(1f)
		)
	}
}