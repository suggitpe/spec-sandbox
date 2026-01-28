package com.recipemanager.presentation.viewmodel

import com.recipemanager.presentation.navigation.StatePersistence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Base ViewModel class providing common functionality for state management and persistence.
 * Implements Requirements 7.5 (state preservation) and 8.1, 8.2 (data persistence).
 */
abstract class BaseViewModel<T : Any>(
    initialState: T,
    private val statePersistence: StatePersistence? = null,
    private val stateKey: String? = null
) {
    // Use SupervisorJob to prevent child coroutine failures from cancelling the entire scope
    protected val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<T> = _state.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    protected var currentState: T
        get() = _state.value
        set(value) {
            _state.value = value
            // Auto-persist state if persistence is configured
            if (statePersistence != null && stateKey != null) {
                persistState()
            }
        }
    
    /**
     * Initialize the ViewModel and restore state if available.
     */
    open fun initialize() {
        if (statePersistence != null && stateKey != null) {
            restoreState()
        }
        onInitialize()
    }
    
    /**
     * Override this method to perform initialization logic after state restoration.
     */
    protected open fun onInitialize() {}
    
    /**
     * Set loading state.
     */
    protected fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
    
    /**
     * Set error state.
     */
    protected fun setError(error: String?) {
        _error.value = error
    }
    
    /**
     * Clear error state.
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Persist current state to storage.
     */
    private fun persistState() {
        if (statePersistence == null || stateKey == null) return
        
        viewModelScope.launch {
            try {
                val serializedState = serializeState(currentState)
                if (serializedState != null) {
                    statePersistence.saveState(stateKey, serializedState)
                }
            } catch (e: Exception) {
                // Log error but don't crash
                println("Failed to persist state for $stateKey: ${e.message}")
            }
        }
    }
    
    /**
     * Restore state from storage.
     */
    private fun restoreState() {
        if (statePersistence == null || stateKey == null) return
        
        viewModelScope.launch {
            try {
                val serializedState = statePersistence.loadState(stateKey)
                if (serializedState != null) {
                    val restoredState = deserializeState(serializedState)
                    if (restoredState != null) {
                        _state.value = restoredState
                    }
                }
            } catch (e: Exception) {
                // Log error but continue with default state
                println("Failed to restore state for $stateKey: ${e.message}")
            }
        }
    }
    
    /**
     * Override this method to provide custom state serialization.
     * Default implementation returns null (no persistence).
     */
    protected open fun serializeState(state: T): String? = null
    
    /**
     * Override this method to provide custom state deserialization.
     * Default implementation returns null (no persistence).
     */
    protected open fun deserializeState(serializedState: String): T? = null
    
    /**
     * Clean up resources when ViewModel is no longer needed.
     */
    open fun onCleared() {
        viewModelScope.cancel()
    }
    
    /**
     * Handle app going to background - persist state.
     */
    open fun onAppPaused() {
        if (statePersistence != null && stateKey != null) {
            persistState()
        }
    }
    
    /**
     * Handle app coming to foreground - optionally refresh data.
     */
    open fun onAppResumed() {
        // Override in subclasses if needed
    }
}

/**
 * Serializable wrapper for states that need persistence.
 */
@Serializable
data class PersistableState<T>(
    val data: T,
    val timestamp: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
)

/**
 * Helper function to create a persistable state wrapper.
 */
fun <T> T.toPersistable(): PersistableState<T> = PersistableState(this)

/**
 * Helper function to extract data from persistable state.
 */
fun <T> PersistableState<T>.extractData(): T = data