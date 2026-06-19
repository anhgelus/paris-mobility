package world.anhgelus.parismobility.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import world.anhgelus.parismobility.models.LinesGroupDataState

class LinesRepository(
    private val linesSource: LinesDataSource,
    private val prismSource: PrimDataSource
) {

    private val _lines = MutableStateFlow<List<LinesGroupDataState>>(listOf())
    val lines = _lines.asStateFlow()

    private val _disruptions = MutableStateFlow<Map<String, List<Disruption>>>(mapOf())
    val disruptions = _disruptions.asStateFlow()

//    val bus = newGroup("Bus", "", R.raw.bus)

    suspend fun initLines(ctx: Context) {
        updateLines(linesSource.getLinesGroups(ctx))
    }

    suspend fun updateDisruptions() {
        prismSource.disruptions.collectLatest {
            _disruptions.value = it
            updateLines()
        }
    }

    private fun updateLines(lines: List<LinesGroupDataState> = _lines.value) {
        _lines.value = lines.map { kind ->
            val lines = kind.lines.map {
                it.copy(disruption = disruptions.value[it.id]?.sorted()?.max())
            }
            kind.copy(lines = lines)
        }
    }
}