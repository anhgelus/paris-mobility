package world.anhgelus.parismobility.models

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkViewModel(
    linesGroup: List<LinesGroupDataState>,
) : ViewModel() {
    private val _linesGroups = MutableStateFlow(linesGroup)
    val linesGroups = _linesGroups.asStateFlow()
}
