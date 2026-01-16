package com.recipemanager.presentation.viewmodel

import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.repository.RecipeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecipeListState(
    val recipes: List<Recipe> = emptyList(),
    val filteredRecipes: List<Recipe> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class RecipeListViewModel(
    private val recipeRepository: RecipeRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _state = MutableStateFlow(RecipeListState())
    val state: StateFlow<RecipeListState> = _state.asStateFlow()

    init {
        loadRecipes()
    }

    fun loadRecipes() {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            recipeRepository.getAllRecipes()
                .onSuccess { recipes ->
                    _state.value = _state.value.copy(
                        recipes = recipes,
                        filteredRecipes = recipes,
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

    fun searchRecipes(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        
        if (query.isBlank()) {
            _state.value = _state.value.copy(filteredRecipes = _state.value.recipes)
            return
        }

        scope.launch {
            recipeRepository.searchRecipes(query)
                .onSuccess { results ->
                    _state.value = _state.value.copy(filteredRecipes = results)
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        error = error.message ?: "Search failed"
                    )
                }
        }
    }

    fun deleteRecipe(recipeId: String) {
        scope.launch {
            recipeRepository.deleteRecipe(recipeId)
                .onSuccess {
                    loadRecipes()
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        error = error.message ?: "Failed to delete recipe"
                    )
                }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
