package world.anhgelus.parismobility.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import world.anhgelus.parismobility.data.LinesRepository

class NetworkViewModel(
    repo: LinesRepository,
) : ViewModel() {
    val lines = repo.lines
    val disruptions = repo.disruptions.stateIn(
        scope = viewModelScope,
        // Start without waiting a listener, because disruptions are deeply linked with lines
        started = SharingStarted.Eagerly,
        initialValue = emptyMap()
    )
}
