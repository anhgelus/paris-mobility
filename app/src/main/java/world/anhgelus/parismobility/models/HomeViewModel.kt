package world.anhgelus.parismobility.models

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel(
    lines: List<LineDataState>,
    stops: List<StopDataState>
) : ViewModel() {
    private val _lines = MutableStateFlow(lines)
    val lines = _lines.asStateFlow()

    private val _stops = MutableStateFlow(stops)
    val stops = _stops.asStateFlow()
}