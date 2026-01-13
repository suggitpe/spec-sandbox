package com.recipemanager.domain.repository

import com.recipemanager.domain.model.Recipe
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    suspend fun createRecipe(recipe: Recipe): Result<Recipe>
    suspend fun getRecipe(id: String): Result<Recipe?>
    suspend fun updateRecipe(recipe: Recipe): Result<Recipe>
    suspend fun deleteRecipe(id: String): Result<Unit>
    suspend fun searchRecipes(query: String): Result<List<Recipe>>
    fun observeRecipes(): Flow<List<Recipe>>
    suspend fun getAllRecipes(): Result<List<Recipe>>
}