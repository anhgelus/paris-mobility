package world.anhgelus.parismobility.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import world.anhgelus.parismobility.R
import world.anhgelus.parismobility.data.Line
import world.anhgelus.parismobility.data.LineGroups
import world.anhgelus.parismobility.data.LineStops
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
    stops: LineStops,
    savedStops: Collection<SavedStop>,
    onUpdateLines: (LineKind, Line, Boolean) -> Unit,
    onUpdateStops: (LineKind, Line, Stop, Boolean) -> Unit,
) {
    val modifyBackStack = rememberNavBackStack(Route.Home.Modify)

    val routes = listOf(
        Triple(Route.Home.Modify, 0, "Lignes"), Triple(Route.Home.Modify.Stops, 1, "Arrêts")
    )

    val selected = routes.first { it.first == modifyBackStack.last() }.second

    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        SecondaryTabRow(
            selectedTabIndex = selected
        ) {
            routes.forEach { (dest, index, label) ->
                Tab(
                    selected = selected == index,
                    onClick = {
                        modifyBackStack.remove(modifyBackStack.last())
                        modifyBackStack.add(routes.first { it.first == dest }.first)
                    },
                    text = { Text(text = label) }
                )
            }
        }
        NavDisplay(
            backStack = modifyBackStack,
            entryProvider = entryProvider {
                entry<Route.Home.Modify> {
                    ModifySavedLines(groups, savedLines, onUpdateLines)
                }
                entry<Route.Home.Modify.Stops> {
                    ModifySavedStops(groups, stops, savedStops, onUpdateStops)
                }
            }
        )
    }
}

@Composable
fun ModifySavedLines(
    groups: LineGroups,
    savedLines: Collection<SavedLine>,
    onUpdate: (LineKind, Line, Boolean) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        groups.filter { it.key != LineKind.BUS }.forEach { (kind, lines) ->
            item {
                SectionTitle(
                    content = kind.displayName,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            items(lines) {
                Select(
                    checked = savedLines.contains(kind, it),
                    onUpdate = { change ->
                        onUpdate(kind, it, change)
                    }
                ) { m ->
                    LineDetailed(kind, it, m)
                }
            }
        }
    }
}

@Composable
fun ModifySavedStops(
    groups: LineGroups,
    stops: LineStops,
    savedStops: Collection<SavedStop>,
    onUpdateStops: (LineKind, Line, Stop, Boolean) -> Unit,
) {
    val modifyBackStack = rememberNavBackStack(Route.Home.Modify.Stops)
    NavDisplay(
        backStack = modifyBackStack,
        entryProvider = entryProvider {
            entry<Route.Home.Modify.Stops> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    groups.filter { it.key != LineKind.BUS }.forEach { (kind, lines) ->
                        item {
                            SectionTitle(
                                content = kind.displayName,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                        items(lines) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = {
                                        modifyBackStack.add(Route.Home.Modify.Stop(kind, it))
                                    }),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                LineDetailed(kind, it)
                                Icon(
                                    painter = painterResource(R.drawable.baseline_keyboard_arrow_right_24),
                                    contentDescription = "Continue"
                                )
                            }
                        }
                    }
                }
            }
            entry<Route.Home.Modify.Stop> { (kind, line) ->
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    items(stops[line.id] ?: listOf()) {
                        Select(
                            checked = savedStops.contains(kind, it),
                            onUpdate = { change ->
                                onUpdateStops(kind, line, it, change)
                            }
                        ) { m ->
                            Text(
                                text = it.name + " (direction )",
                                softWrap = true,
                                modifier = m.fillMaxWidth()
                            ) //TODO: add direction
                        }
                    }
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