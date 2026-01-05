package foundation.rosenblueth.library.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import foundation.rosenblueth.library.data.model.CaptureData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * DataStore para gestionar el historial de libros capturados
 */
class CaptureDataStore(private val context: Context) {
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "capture_history")
        private val CAPTURE_HISTORY = stringPreferencesKey("capture_history")
    }
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * Guarda un libro capturado en el historial
     */
    suspend fun saveCapturedBook(captureData: CaptureData) {
        try {
            val currentList = getCaptureHistoryList()
            
            // Evitar duplicados basados en el ID
            val updatedList = currentList.filter { it.id != captureData.id }.toMutableList()
            updatedList.add(0, captureData) // Añadir al principio de la lista
            
            // Limitar a los últimos 100 libros
            val limitedList = updatedList.take(100)
            
            val jsonString = json.encodeToString(limitedList)
            
            context.dataStore.edit { preferences ->
                preferences[CAPTURE_HISTORY] = jsonString
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Obtiene el historial de libros capturados como Flow
     */
    fun getCaptureHistory(): Flow<List<CaptureData>> {
        return context.dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { preferences ->
                try {
                    val historyJson = preferences[CAPTURE_HISTORY] ?: "[]"
                    json.decodeFromString<List<CaptureData>>(historyJson)
                } catch (e: Exception) {
                    emptyList()
                }
            }
    }
    
    /**
     * Obtiene el historial como lista (suspending function)
     */
    private suspend fun getCaptureHistoryList(): List<CaptureData> {
        return try {
            context.dataStore.data
                .catch { emit(emptyPreferences()) }
                .map { preferences ->
                    try {
                        val historyJson = preferences[CAPTURE_HISTORY] ?: "[]"
                        json.decodeFromString<List<CaptureData>>(historyJson)
                    } catch (_: Exception) {
                        emptyList<CaptureData>()
                    }
                }
                .first()
        } catch (_: Exception) {
            emptyList()
        }
    }
    
    /**
     * Elimina un libro del historial
     */
    suspend fun deleteBook(id: String) {
        try {
            val currentList = getCaptureHistoryList()
            val updatedList = currentList.filter { it.id != id }
            val jsonString = json.encodeToString(updatedList)
            
            context.dataStore.edit { preferences ->
                preferences[CAPTURE_HISTORY] = jsonString
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Limpia todo el historial
     */
    suspend fun clearHistory() {
        try {
            context.dataStore.edit { preferences ->
                preferences[CAPTURE_HISTORY] = "[]"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
