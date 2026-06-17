package world.anhgelus.parismobility.models

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Stable
data class LineDataState(
    val name: String,
    val resource: Int,
    val issue: LineIssue? = null,
)

@Stable
data class LineIssue(
    val level: LineIssueLevel,
    val details: String? = null
)

enum class LineIssueLevel(
    val modifier: @Composable ((Modifier) -> Modifier) = { it },
) {
    NORMAL,
    MINOR_ISSUE({
        it.border(4.dp, MaterialTheme.colorScheme.error)
    }),
    MAJOR_ISSUE({
        it.border(4.dp, MaterialTheme.colorScheme.errorContainer)
    }),
}