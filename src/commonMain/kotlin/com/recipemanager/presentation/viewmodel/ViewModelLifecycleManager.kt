package com.recipemanager.presentation.viewmodel

import com.recipemanager.presentation.navigation.StatePersistence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages the lifecycle of ViewModels and coordinates state persistence.
 * Implements Requirements 7.5 (state preservation) and 8.1, 8.2 (data persistence).
 */
class ViewModelLifecycleManager(
    private val statePersistence: StatePersistence
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val activeViewModels = mutableMapOf<String, BaseViewModel<*>>()
    
    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()
    
    /**
     * Register a ViewModel for lifecycle management.
     */
    fun <T : Any> registerViewModel(
        key: String,
        viewModel: BaseViewModel<T>
    ) {
        activeViewModels[key] = viewModel
        viewModel.initialize()
    }
    
    /**
     * Unregister a ViewModel and clean up resources.
     */
    fun unregisterViewModel(key: String) {
        activeViewModels[key]?.onCleared()
        activeViewModels.remove(key)
    }
    
    /**
     * Handle application going to background.
     * Persists state for all active ViewModels.
     */
    fun onAppPaused() {
        scope.launch {
            _isRestoring.value = true
            try {
                activeViewModels.values.forEach { viewModel ->
                    viewModel.onAppPaused()
                }
            } finally {
                _isRestoring.value = false
            }
        }
    }
    
    /**
     * Handle application coming to foreground.
     * Allows ViewModels to refresh data if needed.
     */
    fun onAppResumed() {
        activeViewModels.values.forEach { viewModel ->
            viewModel.onAppResumed()
        }
    }
    
    /**
     * Clear all persisted state (for logout, reset, etc.).
     */
    suspend fun clearAllPersistedState() {
        // Clear state for all known ViewModels
        val viewModelKeys = listOf(
            "recipe_list",
            "recipe_detail",
            "recipe_form",
            "cooking_mode",
            "photo_management",
            "collection_list",
            "collection_detail",
            "share",
            "import"
        )
        
        viewModelKeys.forEach { key ->
            try {
                statePersistence.clearState(key)
            } catch (e: Exception) {
                println("Failed to clear state for $key: ${e.message}")
            }
        }
    }
    
    /**
     * Get the count of active ViewModels.
     */
    fun getActiveViewModelCount(): Int = activeViewModels.size
    
    /**
     * Clean up all ViewModels and resources.
     */
    fun cleanup() {
        activeViewModels.values.forEach { it.onCleared() }
        activeViewModels.clear()
    }
}

/**
 * Application-level state that persists across sessions.
 * Requirements 8.1, 8.2: Data persistence across app sessions.
 */
@kotlinx.serialization.Serializable
data class AppSessionState(
    val lastActiveScreen: String? = null,
    val lastRecipeId: String? = null,
    val lastCollectionId: String? = null,
    val searchQuery: String? = null,
    val cookingSessionActive: Boolean = false,
    val activeTimerCount: Int = 0,
    val timestamp: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
)

/**
 * Manages application-level session state.
 */
class AppSessionManager(
    private val statePersistence: StatePersistence,
    private val lifecycleManager: ViewModelLifecycleManager
) {
    companion object {
        private const val SESSION_STATE_KEY = "app_session_state"
    }
    
    private val _sessionState = MutableStateFlow(AppSessionState())
    val sessionState: StateFlow<AppSessionState> = _sessionState.asStateFlow()
    
    /**
     * Initialize session manager and restore session state.
     */
    suspend fun initialize() {
        try {
            val serializedState = statePersistence.loadState(SESSION_STATE_KEY)
            if (serializedState != null) {
                val restoredState = kotlinx.serialization.json.Json.decodeFromString(
                    AppSessionState.serializer(),
                    serializedState
                )
                _sessionState.value = restoredState
            }
        } catch (e: Exception) {
            println("Failed to restore session state: ${e.message}")
        }
    }
    
    /**
     * Update session state and persist it.
     */
    fun updateSessionState(update: (AppSessionState) -> AppSessionState) {
        _sessionState.value = update(_sessionState.value)
        persistSessionState()
    }
    
    /**
     * Persist current session state.
     */
    private fun persistSessionState() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val serializedState = kotlinx.serialization.json.Json.encodeToString(
                    AppSessionState.serializer(),
                    _sessionState.value
                )
                statePersistence.saveState(SESSION_STATE_KEY, serializedState)
            } catch (e: Exception) {
                println("Failed to persist session state: ${e.message}")
            }
        }
    }
    
    /**
     * Clear session state.
     */
    suspend fun clearSession() {
        _sessionState.value = AppSessionState()
        statePersistence.clearState(SESSION_STATE_KEY)
        lifecycleManager.clearAllPersistedState()
    }
}