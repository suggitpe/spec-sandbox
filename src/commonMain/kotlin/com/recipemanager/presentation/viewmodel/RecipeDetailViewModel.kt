package com.recipemanager.presentation.viewmodel

import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.repository.RecipeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecipeDetailState(
    val recipe: Recipe? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class RecipeDetailViewModel(
    private val recipeRepository: RecipeRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _state = MutableStateFlow(RecipeDetailState())
    val state: StateFlow<RecipeDetailState> = _state.asStateFlow()

    fun loadRecipe(recipeId: String) {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            recipeRepository.getRecipe(recipeId)
                .onSuccess { recipe ->
                    _state.value = _state.value.copy(
                        recipe = recipe,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load recipe"
                    )
                }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
