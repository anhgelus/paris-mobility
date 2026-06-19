package world.anhgelus.parismobility.models

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import world.anhgelus.parismobility.data.Line
import world.anhgelus.parismobility.data.LinesRepository

class HomeViewModel(
    lines: List<Line>,
    stops: List<StopDataState>,
    val repo: LinesRepository,
) : ViewModel() {
    private val _lines = MutableStateFlow(lines)
    val lines = _lines.asStateFlow()

    private val _stops = MutableStateFlow(stops)
    val stops = _stops.asStateFlow()
}