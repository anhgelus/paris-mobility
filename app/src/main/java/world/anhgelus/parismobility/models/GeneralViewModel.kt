package world.anhgelus.parismobility.models

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import world.anhgelus.parismobility.data.LinesRepository
import world.anhgelus.parismobility.data.PreferencesRepository

class GeneralViewModel(ctx: Context) : ViewModel() {
	val linesRepository = LinesRepository.getInstance(ctx)
	val preferencesRepository = PreferencesRepository(ctx)
	val isConnected = linesRepository.backendSource.isConnected

	val disruptions = linesRepository.disruptions.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(5_000),
		initialValue = emptyMap()
	)
}