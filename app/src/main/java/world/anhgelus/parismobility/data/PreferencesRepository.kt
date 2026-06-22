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
        Json.decodeFromString(raw)
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

    val stopsFlow: Flow<Set<SavedStop>> = ctx.dataStore.data.map { preferences ->
        val raw = preferences[SAVED_STOPS] ?: return@map emptySet()
        Json.decodeFromString(raw)
    }

    suspend fun addStops(ctx: Context, vararg stops: SavedStop) {
        ctx.dataStore.updateData {
            it.toMutablePreferences().also { prefs ->
                val saved = stopsFlow.first().toMutableSet()
                saved.addAll(stops)
                prefs[SAVED_STOPS] = Json.encodeToString(saved)
            }
        }
    }

    suspend fun removeStops(ctx: Context, vararg stops: SavedStop) {
        ctx.dataStore.updateData {
            it.toMutablePreferences().also { prefs ->
                val saved = stopsFlow.first().toMutableSet()
                saved.removeAll(stops.toSet())
                prefs[SAVED_STOPS] = Json.encodeToString(saved)
            }
        }
    }

    companion object {
        val SAVED_LINES = stringPreferencesKey("saved_lines")
        val SAVED_STOPS = stringPreferencesKey("saved_stops")
    }
}

@Serializable
data class SavedLine(
    val kind: LineKind,
    val line: String,
)

fun Collection<SavedLine>.contains(kind: LineKind, line: Line): Boolean {
    return this.contains(SavedLine(kind, line.id))
}

@Serializable
data class SavedStop(
    val line: SavedLine,
    val stop: Int,
    val direction: String,
)

fun Collection<SavedStop>.contains(kind: LineKind, stop: Stop): Boolean {
    return this.contains(SavedStop(SavedLine(kind, stop.line), stop.id, ""))
}