package world.anhgelus.parismobility.models

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import world.anhgelus.parismobility.data.LinesDataSource
import world.anhgelus.parismobility.data.LinesRepository
import world.anhgelus.parismobility.data.PreferencesRepository
import world.anhgelus.parismobility.data.backend.BackendDataSource

class GeneralViewModel(
	ctx: Context,
	linesDataSource: LinesDataSource,
	backendDataSource: BackendDataSource,
) : ViewModel() {
	private val _isLoading = MutableStateFlow(true)
	val isLoading = _isLoading.asStateFlow()

	val linesRepository = LinesRepository(
		linesDataSource,
		backendDataSource
	)
	val preferencesRepository = PreferencesRepository(ctx)

	init {
		loadData(ctx)
	}

	fun loadData(ctx: Context) {
		_isLoading.update { true }
		viewModelScope.launch {
			linesRepository.loadLines(ctx.resources)
			_isLoading.update { false }
			// after isLoading because this is not a required to start
			linesRepository.loadStops(ctx.resources)
		}
	}
}