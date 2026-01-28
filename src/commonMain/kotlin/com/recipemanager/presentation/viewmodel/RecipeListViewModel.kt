package com.recipemanager.presentation.viewmodel

import com.recipemanager.domain.model.Recipe
import com.recipemanager.domain.repository.RecipeRepository
import com.recipemanager.presentation.navigation.StatePersistence
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class RecipeListState(
    val recipes: List<Recipe> = emptyList(),
    val filteredRecipes: List<Recipe> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastRefreshTime: Long = 0L
)

class RecipeListViewModel(
    private val recipeRepository: RecipeRepository,
    statePersistence: StatePersistence? = null
) : BaseViewModel<RecipeListState>(
    initialState = RecipeListState(),
    statePersistence = statePersistence,
    stateKey = "recipe_list"
) {
    
    override fun onInitialize() {
        // Load recipes if not recently loaded or if no recipes cached
        val shouldRefresh = currentState.recipes.isEmpty() || 
            (System.currentTimeMillis() - currentState.lastRefreshTime) > 300_000 // 5 minutes
        
        if (shouldRefresh) {
            loadRecipes()
        }
    }

    fun loadRecipes() {
        viewModelScope.launch {
            setLoading(true)
            setError(null)
            
            recipeRepository.getAllRecipes()
                .onSuccess { recipes ->
                    currentState = currentState.copy(
                        recipes = recipes,
                        filteredRecipes = if (currentState.searchQuery.isBlank()) recipes 
                                         else filterRecipes(recipes, currentState.searchQuery),
                        isLoading = false,
                        lastRefreshTime = System.currentTimeMillis()
                    )
                    setLoading(false)
                }
                .onFailure { error ->
                    setError(error.message ?: "Failed to load recipes")
                    setLoading(false)
                }
        }
    }

    fun searchRecipes(query: String) {
        currentState = currentState.copy(searchQuery = query)
        
        if (query.isBlank()) {
            currentState = currentState.copy(filteredRecipes = currentState.recipes)
            return
        }

        viewModelScope.launch {
            recipeRepository.searchRecipes(query)
                .onSuccess { results ->
                    currentState = currentState.copy(filteredRecipes = results)
                }
                .onFailure { error ->
                    setError(error.message ?: "Search failed")
                }
        }
    }
    
    private fun filterRecipes(recipes: List<Recipe>, query: String): List<Recipe> {
        return recipes.filter { recipe ->
            recipe.title.contains(query, ignoreCase = true) ||
            recipe.description?.contains(query, ignoreCase = true) == true ||
            recipe.tags.any { it.contains(query, ignoreCase = true) } ||
            recipe.ingredients.any { it.name.contains(query, ignoreCase = true) }
        }
    }

    fun deleteRecipe(recipeId: String) {
        viewModelScope.launch {
            recipeRepository.deleteRecipe(recipeId)
                .onSuccess {
                    loadRecipes()
                }
                .onFailure { error ->
                    setError(error.message ?: "Failed to delete recipe")
                }
        }
    }
    
    override fun onAppResumed() {
        // Refresh recipes when app comes back to foreground
        loadRecipes()
    }
    
    override fun serializeState(state: RecipeListState): String? {
        return try {
            Json.encodeToString(state)
        } catch (e: Exception) {
            null
        }
    }
    
    override fun deserializeState(serializedState: String): RecipeListState? {
        return try {
            Json.decodeFromString<RecipeListState>(serializedState)
        } catch (e: Exception) {
            null
        }
    }
}
