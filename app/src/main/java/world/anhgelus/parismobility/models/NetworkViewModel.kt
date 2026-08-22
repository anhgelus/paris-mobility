package world.anhgelus.parismobility.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import world.anhgelus.parismobility.data.LinesRepository
import world.anhgelus.parismobility.data.Result

class NetworkViewModel(
    repo: LinesRepository,
) : ViewModel() {
    val lines = repo.lines
    val disruptions = repo.disruptions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(30 * 1000, 2 * 60 * 1000),
        initialValue = Result.Ok(emptyMap())
    )
}
