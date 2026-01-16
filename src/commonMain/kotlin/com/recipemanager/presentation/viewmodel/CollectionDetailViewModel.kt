package com.recipemanager.presentation.viewmodel

import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.model.RecipeCollection
import com.recipemanager.domain.repository.CollectionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class CollectionDetailState(
    val collection: RecipeCollection? = null,
    val recipes: List<Recipe> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditing: Boolean = false,
    val editName: String = "",
    val editDescription: String = ""
)

class CollectionDetailViewModel(
    private val collectionId: String,
    private val collectionRepository: CollectionRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _state = MutableStateFlow(CollectionDetailState())
    val state: StateFlow<CollectionDetailState> = _state.asStateFlow()

    init {
        loadCollection()
    }

    fun loadCollection() {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            collectionRepository.getCollection(collectionId)
                .onSuccess { collection ->
                    if (collection != null) {
                        _state.value = _state.value.copy(
                            collection = collection,
                            editName = collection.name,
                            editDescription = collection.description ?: ""
                        )
                        loadRecipes()
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Collection not found"
                        )
                    }
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load collection"
                    )
                }
        }
    }

    private fun loadRecipes() {
        scope.launch {
            collectionRepository.getRecipesInCollection(collectionId)
                .onSuccess { recipes ->
                    _state.value = _state.value.copy(
                        recipes = recipes,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load recipes"
                    )
                }
        }
    }

    fun startEditing() {
        _state.value = _state.value.copy(isEditing = true)
    }

    fun cancelEditing() {
        val collection = _state.value.collection
        _state.value = _state.value.copy(
            isEditing = false,
            editName = collection?.name ?: "",
            editDescription = collection?.description ?: ""
        )
    }

    fun updateName(name: String) {
        _state.value = _state.value.copy(editName = name)
    }

    fun updateDescription(description: String) {
        _state.value = _state.value.copy(editDescription = description)
    }

    fun saveChanges() {
        val collection = _state.value.collection ?: return
        
        scope.launch {
            val updatedCollection = collection.copy(
                name = _state.value.editName,
                description = _state.value.editDescription.takeIf { it.isNotBlank() },
                updatedAt = Clock.System.now()
            )
            
            collectionRepository.updateCollection(updatedCollection)
                .onSuccess {
                    _state.value = _state.value.copy(
                        collection = updatedCollection,
                        isEditing = false
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        error = error.message ?: "Failed to update collection"
                    )
                }
        }
    }

    fun removeRecipe(recipeId: String) {
        scope.launch {
            collectionRepository.removeRecipeFromCollection(recipeId, collectionId)
                .onSuccess {
                    loadCollection()
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        error = error.message ?: "Failed to remove recipe"
                    )
                }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
