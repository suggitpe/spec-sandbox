package com.recipemanager.presentation.viewmodel

import com.recipemanager.domain.model.RecipeCollection
import com.recipemanager.domain.repository.CollectionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CollectionListState(
    val collections: List<RecipeCollection> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class CollectionListViewModel(
    private val collectionRepository: CollectionRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _state = MutableStateFlow(CollectionListState())
    val state: StateFlow<CollectionListState> = _state.asStateFlow()

    init {
        loadCollections()
    }

    fun loadCollections() {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            collectionRepository.getAllCollections()
                .onSuccess { collections ->
                    _state.value = _state.value.copy(
                        collections = collections,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load collections"
                    )
                }
        }
    }

    fun deleteCollection(collectionId: String) {
        scope.launch {
            collectionRepository.deleteCollection(collectionId)
                .onSuccess {
                    loadCollections()
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        error = error.message ?: "Failed to delete collection"
                    )
                }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
