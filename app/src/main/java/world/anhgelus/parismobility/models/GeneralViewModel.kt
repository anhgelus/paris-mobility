package world.anhgelus.parismobility.models

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import world.anhgelus.parismobility.data.LinesDataSource
import world.anhgelus.parismobility.data.LinesRepository
import world.anhgelus.parismobility.data.PreferencesRepository
import world.anhgelus.parismobility.data.backend.BackendDataSource

class GeneralViewModel(
	ctx: Context,
	linesDataSource: LinesDataSource,
	backendDataSource: BackendDataSource,
) : ViewModel() {
	val linesRepository = LinesRepository(
		linesDataSource,
		backendDataSource
	)
	val preferencesRepository = PreferencesRepository(ctx)

	val disruptions = linesRepository.disruptions.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(5_000),
		initialValue = emptyMap()
	)

	init {
		linesRepository.loadLines()
	}
}