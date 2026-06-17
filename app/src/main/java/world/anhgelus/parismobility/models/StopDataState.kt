package world.anhgelus.parismobility.models

import androidx.compose.runtime.Stable

@Stable
data class StopDataState(
    val name: String,
    val line: LineDataState,
    val nextTrains: Collection<Pair<String, List<String>>>
)