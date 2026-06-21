package world.anhgelus.parismobility.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import world.anhgelus.parismobility.models.LineKind

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "preferences")

class PreferencesRepository(ctx: Context) {
    val linesFlow: Flow<Set<SavedLine>> = ctx.dataStore.data.map { preferences ->
        val raw = preferences[SAVED_LINES] ?: return@map emptySet()
        Json.decodeFromString<Set<SavedLine>>(raw)
    }

    suspend fun addLines(ctx: Context, vararg lines: SavedLine) {
        ctx.dataStore.updateData {
            it.toMutablePreferences().also { prefs ->
                val saved = linesFlow.first().toMutableSet()
                saved.addAll(lines)
                prefs[SAVED_LINES] = Json.encodeToString(saved)
            }
        }
    }

    suspend fun removeLines(ctx: Context, vararg lines: SavedLine) {
        ctx.dataStore.updateData {
            it.toMutablePreferences().also { prefs ->
                val saved = linesFlow.first().toMutableSet()
                saved.removeAll(lines.toSet())
                prefs[SAVED_LINES] = Json.encodeToString(saved)
            }
        }
    }

    companion object {
        val SAVED_LINES = stringPreferencesKey("saved_lines")
    }
}

@Serializable
data class SavedLine(
    val kind: LineKind,
    val line: String,
)

fun Map<LineKind, List<Line>>.getLine(saved: SavedLine): Pair<LineKind, Line>? {
    val line = this[saved.kind]?.firstOrNull { it.id == saved.line } ?: return null
    return Pair(saved.kind, line)
}

fun Collection<SavedLine>.contains(kind: LineKind, line: Line): Boolean {
    return this.contains(SavedLine(kind, line.id))
}