package world.anhgelus.parismobility.models

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import world.anhgelus.parismobility.data.Line
import world.anhgelus.parismobility.data.LinesRepository
import world.anhgelus.parismobility.data.PreferencesRepository
import world.anhgelus.parismobility.data.SavedLine

class HomeViewModel(
    private val preferencesRepo: PreferencesRepository,
    private val linesRepo: LinesRepository,
    stops: List<StopDataState>,
) : ViewModel() {
    val lines = linesRepo.lines

    @Composable
    fun getSavedLines(): List<Line> {
        val lines by lines.collectAsStateWithLifecycle()
        val savedLines by preferencesRepo.linesFlow.collectAsStateWithLifecycle(emptySet())
        return savedLines.mapNotNull { it.toLine(lines) }
    }

    private val _stops = MutableStateFlow(stops)
    val stops = _stops.asStateFlow()

    fun saveLine(ctx: Context, kind: LineKind, line: Line) {
        viewModelScope.launch {
            preferencesRepo.addLines(ctx, SavedLine(kind, line.id))
        }
    }

    fun removeLine(ctx: Context, kind: LineKind, line: Line) {
        viewModelScope.launch {
            preferencesRepo.removeLines(ctx, SavedLine(kind, line.id))
        }
    }
}