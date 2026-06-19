package world.anhgelus.parismobility.models

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import world.anhgelus.parismobility.data.LinesRepository

data class NetworkViewModel(
    val repo: LinesRepository,
    val linesGroup: StateFlow<List<LinesGroupDataState>>,
) : ViewModel()
