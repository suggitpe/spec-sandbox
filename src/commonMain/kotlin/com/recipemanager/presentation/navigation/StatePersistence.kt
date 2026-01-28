package com.recipemanager.presentation.navigation

import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Interface for persisting application state across sessions.
 * Platform-specific implementations will handle actual storage.
 */
interface StatePersistence {
    suspend fun saveState(key: String, value: String)
    suspend fun loadState(key: String): String?
    suspend fun clearState(key: String)
}

/**
 * In-memory implementation of StatePersistence for testing and fallback.
 * Production implementations should use platform-specific storage.
 */
class InMemoryStatePersistence : StatePersistence {
    private val storage = mutableMapOf<String, String>()
    
    override suspend fun saveState(key: String, value: String) {
        storage[key] = value
    }
    
    override suspend fun loadState(key: String): String? {
        return storage[key]
    }
    
    override suspend fun clearState(key: String) {
        storage.remove(key)
    }
}

/**
 * Manages application state persistence including navigation state.
 * Implements Requirements 7.5 (state preservation) and 8.1, 8.2 (data persistence).
 */
class AppStatePersistenceManager(
    private val statePersistence: StatePersistence = InMemoryStatePersistence()
) {
    companion object {
        private const val NAVIGATION_STATE_KEY = "navigation_state"
        private const val APP_STATE_KEY = "app_state"
    }
    
    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()
    
    /**
     * Save navigation state for persistence across app sessions.
     */
    suspend fun saveNavigationState(navigationState: NavigationState) {
        try {
            val serializedState = kotlinx.serialization.json.Json.encodeToString(
                NavigationState.serializer(),
                navigationState
            )
            statePersistence.saveState(NAVIGATION_STATE_KEY, serializedState)
        } catch (e: Exception) {
            // Log error but don't crash the app
            println("Failed to save navigation state: ${e.message}")
        }
    }
    
    /**
     * Load navigation state from persistent storage.
     */
    suspend fun loadNavigationState(): NavigationState? {
        return try {
            _isRestoring.value = true
            val serializedState = statePersistence.loadState(NAVIGATION_STATE_KEY)
            serializedState?.let {
                kotlinx.serialization.json.Json.decodeFromString(
                    NavigationState.serializer(),
                    it
                )
            }
        } catch (e: Exception) {
            // Log error and return null to use default state
            println("Failed to load navigation state: ${e.message}")
            null
        } finally {
            _isRestoring.value = false
        }
    }
    
    /**
     * Clear all persisted state.
     */
    suspend fun clearAllState() {
        try {
            statePersistence.clearState(NAVIGATION_STATE_KEY)
            statePersistence.clearState(APP_STATE_KEY)
        } catch (e: Exception) {
            println("Failed to clear state: ${e.message}")
        }
    }
    
    /**
     * Save general application state (for future use).
     */
    suspend fun saveAppState(state: Map<String, String>) {
        try {
            // For now, we'll serialize as a simple JSON object
            val jsonString = state.entries.joinToString(
                prefix = "{",
                postfix = "}",
                separator = ","
            ) { "\"${it.key}\":\"${it.value}\"" }
            statePersistence.saveState(APP_STATE_KEY, jsonString)
        } catch (e: Exception) {
            println("Failed to save app state: ${e.message}")
        }
    }
    
    /**
     * Load general application state (for future use).
     */
    suspend fun loadAppState(): Map<String, String>? {
        return try {
            val serializedState = statePersistence.loadState(APP_STATE_KEY)
            // For now, return null - full JSON parsing would be implemented later
            null
        } catch (e: Exception) {
            println("Failed to load app state: ${e.message}")
            null
        }
    }
}