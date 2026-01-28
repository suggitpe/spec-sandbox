package com.recipemanager.presentation.viewmodel

import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.repository.RecipeRepository
import com.recipemanager.presentation.navigation.StatePersistence
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class RecipeDetailState(
    val recipeId: String? = null,
    val recipe: Recipe? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastLoadTime: Long = 0L
)

class RecipeDetailViewModel(
    private val recipeRepository: RecipeRepository,
    statePersistence: StatePersistence? = null
) : BaseViewModel<RecipeDetailState>(
    initialState = RecipeDetailState(),
    statePersistence = statePersistence,
    stateKey = "recipe_detail"
) {
    
    override fun onInitialize() {
        // If we have a cached recipe and it's recent, don't reload
        val shouldRefresh = currentState.recipe == null || 
            (System.currentTimeMillis() - currentState.lastLoadTime) > 60_000 // 1 minute
        
        if (shouldRefresh && currentState.recipeId != null) {
            loadRecipe(currentState.recipeId!!)
        }
    }

    fun loadRecipe(recipeId: String) {
        // Update the recipe ID in state for persistence
        currentState = currentState.copy(recipeId = recipeId)
        
        viewModelScope.launch {
            setLoading(true)
            setError(null)
            
            recipeRepository.getRecipe(recipeId)
                .onSuccess { recipe ->
                    currentState = currentState.copy(
                        recipe = recipe,
                        isLoading = false,
                        lastLoadTime = System.currentTimeMillis()
                    )
                    setLoading(false)
                }
                .onFailure { error ->
                    setError(error.message ?: "Failed to load recipe")
                    setLoading(false)
                }
        }
    }
    
    fun refreshRecipe() {
        currentState.recipeId?.let { recipeId ->
            loadRecipe(recipeId)
        }
    }
    
    override fun onAppResumed() {
        // Refresh recipe when app comes back to foreground
        refreshRecipe()
    }
    
    override fun serializeState(state: RecipeDetailState): String? {
        return try {
            Json.encodeToString(state)
        } catch (e: Exception) {
            null
        }
    }
    
    override fun deserializeState(serializedState: String): RecipeDetailState? {
        return try {
            Json.decodeFromString<RecipeDetailState>(serializedState)
        } catch (e: Exception) {
            null
        }
    }
}
