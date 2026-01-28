package com.recipemanager.presentation.viewmodel

import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.repository.RecipeRepository
import com.recipemanager.domain.service.ShareService
import com.recipemanager.presentation.navigation.StatePersistence
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
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
    statePersistence: StatePersistence? = null
) : BaseViewModel<ImportState>(
    initialState = ImportState(),
    statePersistence = statePersistence,
    stateKey = "import"
) {

    fun updateImportText(text: String) {
        currentState = currentState.copy(
            importText = text,
            previewRecipe = null,
            error = null
        )
    }

    fun validateAndPreview() {
        val text = currentState.importText
        if (text.isBlank()) {
            setError("Please enter recipe data")
            return
        }
        
        viewModelScope.launch {
            currentState = currentState.copy(isValidating = true)
            setError(null)
            
            shareService.importRecipe(text)
                .onSuccess { recipe ->
                    currentState = currentState.copy(
                        previewRecipe = recipe,
                        isValidating = false
                    )
                }
                .onFailure { error ->
                    currentState = currentState.copy(isValidating = false)
                    setError(error.message ?: "Invalid recipe data")
                }
        }
    }

    fun importRecipe() {
        val recipe = currentState.previewRecipe ?: return
        
        viewModelScope.launch {
            currentState = currentState.copy(isImporting = true)
            setError(null)
            
            // Create a new recipe with updated timestamps and new ID
            val now = Clock.System.now()
            val importedRecipe = recipe.copy(
                id = generateRecipeId(),
                createdAt = now,
                updatedAt = now
            )
            
            recipeRepository.createRecipe(importedRecipe)
                .onSuccess {
                    currentState = currentState.copy(
                        isImporting = false,
                        importSuccess = true
                    )
                }
                .onFailure { error ->
                    currentState = currentState.copy(isImporting = false)
                    setError(error.message ?: "Failed to import recipe")
                }
        }
    }

    fun reset() {
        currentState = ImportState()
    }

    private fun generateRecipeId(): String {
        return "recipe_${Clock.System.now().toEpochMilliseconds()}_${(0..999).random()}"
    }
    
    override fun serializeState(state: ImportState): String? {
        return try {
            Json.encodeToString(state)
        } catch (e: Exception) {
            null
        }
    }
    
    override fun deserializeState(serializedState: String): ImportState? {
        return try {
            Json.decodeFromString<ImportState>(serializedState)
        } catch (e: Exception) {
            null
        }
    }
}
