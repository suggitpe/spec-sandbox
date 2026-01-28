package com.recipemanager.presentation.viewmodel

import com.recipemanager.domain.model.RecipeCollection
import com.recipemanager.domain.repository.CollectionRepository
import com.recipemanager.presentation.navigation.StatePersistence
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class CollectionListState(
    val collections: List<RecipeCollection> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastRefreshTime: Long = 0L
)

class CollectionListViewModel(
    private val collectionRepository: CollectionRepository,
    statePersistence: StatePersistence? = null
) : BaseViewModel<CollectionListState>(
    initialState = CollectionListState(),
    statePersistence = statePersistence,
    stateKey = "collection_list"
) {
    
    override fun onInitialize() {
        // Load collections if not recently loaded or if no collections cached
        val shouldRefresh = currentState.collections.isEmpty() || 
            (System.currentTimeMillis() - currentState.lastRefreshTime) > 300_000 // 5 minutes
        
        if (shouldRefresh) {
            loadCollections()
        }
    }

    fun loadCollections() {
        viewModelScope.launch {
            setLoading(true)
            setError(null)
            
            collectionRepository.getAllCollections()
                .onSuccess { collections ->
                    currentState = currentState.copy(
                        collections = collections,
                        isLoading = false,
                        lastRefreshTime = System.currentTimeMillis()
                    )
                    setLoading(false)
                }
                .onFailure { error ->
                    setError(error.message ?: "Failed to load collections")
                    setLoading(false)
                }
        }
    }

    fun deleteCollection(collectionId: String) {
        viewModelScope.launch {
            collectionRepository.deleteCollection(collectionId)
                .onSuccess {
                    loadCollections()
                }
                .onFailure { error ->
                    setError(error.message ?: "Failed to delete collection")
                }
        }
    }
    
    override fun onAppResumed() {
        // Refresh collections when app comes back to foreground
        loadCollections()
    }
    
    override fun serializeState(state: CollectionListState): String? {
        return try {
            Json.encodeToString(state)
        } catch (e: Exception) {
            null
        }
    }
    
    override fun deserializeState(serializedState: String): CollectionListState? {
        return try {
            Json.decodeFromString<CollectionListState>(serializedState)
        } catch (e: Exception) {
            null
        }
    }
}
