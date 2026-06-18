package world.anhgelus.parismobility.models

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

data class NetworkViewModel(
    val linesGroup: StateFlow<List<LinesGroupDataState>>,
) : ViewModel()
