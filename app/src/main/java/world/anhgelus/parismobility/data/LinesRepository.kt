package world.anhgelus.parismobility.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import world.anhgelus.parismobility.models.LinesGroupDataState

class LinesRepository(
    private val source: LinesDataSource,
) {
    private val _lines = MutableStateFlow<List<LinesGroupDataState>>(listOf())
    val lines = _lines.asStateFlow()

//    val bus = newGroup("Bus", "", R.raw.bus)

    suspend fun updateLines(ctx: Context) {
        _lines.value = source.getLinesGroups(ctx)
    }
}