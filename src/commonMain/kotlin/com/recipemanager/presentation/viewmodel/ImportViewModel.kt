package com.recipemanager.presentation.viewmodel

import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.repository.RecipeRepository
import com.recipemanager.domain.service.ShareService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class ImportState(
    val importText: String = "",
    val previewRecipe: Recipe? = null,
    val isValidating: Boolean = false,
    val isImporting: Boolean = false,
    val importSuccess: Boolean = false,
    val error: String? = null
)

class ImportViewModel(
    private val recipeRepository: RecipeRepository,
    private val shareService: ShareService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _state = MutableStateFlow(ImportState())
    val state: StateFlow<ImportState> = _state.asStateFlow()

    fun updateImportText(text: String) {
        _state.value = _state.value.copy(
            importText = text,
            previewRecipe = null,
            error = null
        )
    }

    fun validateAndPreview() {
        val text = _state.value.importText
        if (text.isBlank()) {
            _state.value = _state.value.copy(error = "Please enter recipe data")
            return
        }
        
        scope.launch {
            _state.value = _state.value.copy(isValidating = true, error = null)
            
            shareService.importRecipe(text)
                .onSuccess { recipe ->
                    _state.value = _state.value.copy(
                        previewRecipe = recipe,
                        isValidating = false
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isValidating = false,
                        error = error.message ?: "Invalid recipe data"
                    )
                }
        }
    }

    fun importRecipe() {
        val recipe = _state.value.previewRecipe ?: return
        
        scope.launch {
            _state.value = _state.value.copy(isImporting = true, error = null)
            
            // Create a new recipe with updated timestamps and new ID
            val now = Clock.System.now()
            val importedRecipe = recipe.copy(
                id = generateRecipeId(),
                createdAt = now,
                updatedAt = now
            )
            
            recipeRepository.createRecipe(importedRecipe)
                .onSuccess {
                    _state.value = _state.value.copy(
                        isImporting = false,
                        importSuccess = true
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isImporting = false,
                        error = error.message ?: "Failed to import recipe"
                    )
                }
        }
    }

    fun reset() {
        _state.value = ImportState()
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun generateRecipeId(): String {
        return "recipe_${Clock.System.now().toEpochMilliseconds()}_${(0..999).random()}"
    }
}
