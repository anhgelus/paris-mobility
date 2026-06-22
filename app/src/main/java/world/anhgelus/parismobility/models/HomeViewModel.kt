package world.anhgelus.parismobility.models

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import world.anhgelus.parismobility.data.Line
import world.anhgelus.parismobility.data.LineStops
import world.anhgelus.parismobility.data.LinesRepository
import world.anhgelus.parismobility.data.PreferencesRepository
import world.anhgelus.parismobility.data.SavedLine
import world.anhgelus.parismobility.data.SavedStop
import world.anhgelus.parismobility.data.Stop
import world.anhgelus.parismobility.data.get

class HomeViewModel(
    private val preferencesRepo: PreferencesRepository,
    private val linesRepo: LinesRepository,
) : ViewModel() {
    val lines = linesRepo.lines

    val savedLines = preferencesRepo.linesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(60_000),
        initialValue = emptySet()
    )

    val stops: StateFlow<LineStops> = linesRepo.stops.onEach { mp ->
        linesRepo.monitorStop(
            s = savedStops.value.mapNotNull { s -> mp[s]?.second }.toTypedArray(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(60_000),
        initialValue = emptyMap()
    )

    val savedStops: StateFlow<Set<SavedStop>> = preferencesRepo.stopsFlow.onEach { mp ->
        linesRepo.monitorStop(
            s = mp.mapNotNull { s -> stops.value[s]?.second }.toTypedArray(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(60_000),
        initialValue = emptySet()
    )

    val monitoringStops = linesRepo.monitorStops.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(60_000),
        initialValue = emptyMap()
    )

    fun saveLine(ctx: Context, kind: LineKind, line: Line) {
        viewModelScope.launch {
            preferencesRepo.addLines(ctx, SavedLine(kind, line.id))
        }
    }

    fun removeLine(ctx: Context, kind: LineKind, line: Line) {
        viewModelScope.launch {
            preferencesRepo.removeLines(ctx, SavedLine(kind, line.id))
        }
    }

    fun saveStops(ctx: Context, kind: LineKind, line: Line, stop: Stop, direction: String) {
        viewModelScope.launch {
            preferencesRepo.addStops(
                ctx,
                SavedStop(SavedLine(kind, line.id), stop.id, direction)
            )
            linesRepo.monitorStop(stop)
        }
    }

    fun removeStops(ctx: Context, kind: LineKind, line: Line, stop: Stop, direction: String) {
        viewModelScope.launch {
            preferencesRepo.removeStops(
                ctx,
                SavedStop(SavedLine(kind, line.id), stop.id, direction)
            )
            linesRepo.stopMonitoringStop(stop)
        }
    }
}