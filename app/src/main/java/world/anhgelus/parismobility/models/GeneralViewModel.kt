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
import world.anhgelus.parismobility.data.PrimDataSource

class GeneralViewModel(
    ctx: Context,
    val linesDataSource: LinesDataSource,
    val primDataSource: PrimDataSource
) : ViewModel() {
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    lateinit var linesRepository: LinesRepository
        private set
    lateinit var preferencesRepository: PreferencesRepository
        private set

    init {
        loadData(ctx)
    }

    fun loadData(ctx: Context) {
        _isLoading.update { true }
        viewModelScope.launch {
            linesRepository = LinesRepository(linesDataSource, primDataSource)
            preferencesRepository = PreferencesRepository(ctx)
            linesRepository.loadLines(ctx.resources)
            _isLoading.update { false }
            // after isLoading because this is not a required to start
            linesRepository.loadStops(ctx.resources)
        }
    }
}